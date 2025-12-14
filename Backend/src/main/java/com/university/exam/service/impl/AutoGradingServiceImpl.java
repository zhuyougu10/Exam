package com.university.exam.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.utils.DifyClient;
import com.university.exam.entity.MistakeBook;
import com.university.exam.entity.Question;
import com.university.exam.entity.Record;
import com.university.exam.entity.RecordDetail;
import com.university.exam.mapper.RecordMapper; // 核心修改：引入 Mapper
import com.university.exam.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 自动阅卷服务实现类
 * <p>
 * 修复记录：
 * 2025-12-14: 将注入 RecordService 改为 RecordMapper，解决与 RecordServiceImpl 的循环依赖问题。
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoGradingServiceImpl implements AutoGradingService {

    private final DifyClient difyClient;
    
    // ⚠️ 核心修改：这里不再注入 RecordService，而是直接注入 RecordMapper
    // 依赖链变为：RecordServiceImpl -> AutoGradingServiceImpl -> RecordMapper
    // 从而打破了 RecordServiceImpl <-> AutoGradingServiceImpl 的死循环
    private final RecordMapper recordMapper; 
    
    private final RecordDetailService recordDetailService;
    private final QuestionService questionService;
    private final MistakeBookService mistakeBookService;

    @Override
    @Async("aiTaskExecutor") // 使用 AsyncConfig 中配置的线程池
    public void gradeSubjectiveQuestionsAsync(Long recordId) {
        log.info(">>> 开始 AI 自动阅卷: recordId={}", recordId);
        
        try {
            // 1. 获取必要的上下文 (改用 Mapper 查询)
            Record record = recordMapper.selectById(recordId);
            if (record == null) {
                log.warn("考试记录不存在: {}", recordId);
                return;
            }

            // 获取 API Key
            String apiKey = difyClient.getGradingKey();
            if (StrUtil.isBlank(apiKey) || apiKey.contains("******")) {
                log.error("AI 阅卷失败: 未配置有效的 dify_key_grading");
                return;
            }

            // 2. 获取答题明细
            List<RecordDetail> details = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                    .eq(RecordDetail::getRecordId, recordId));

            boolean hasUpdates = false;
            BigDecimal totalScore = BigDecimal.ZERO;
            List<MistakeBook> newMistakes = new ArrayList<>();

            for (RecordDetail detail : details) {
                Question q = questionService.getById(detail.getQuestionId());
                if (q == null) continue;

                // 累加已有的客观题分数
                if (q.getType() == 1 || q.getType() == 2 || q.getType() == 3) {
                    if (detail.getIsMarked() == 1) {
                        totalScore = totalScore.add(detail.getScore());
                    }
                    continue;
                }

                // 3. 筛选主观题 (简答-4, 填空-5) 且未批改的进行 AI 评分
                if ((q.getType() == 4 || q.getType() == 5) && detail.getIsMarked() == 0) {
                    // 调用 Dify
                    Map<String, Object> inputs = new HashMap<>();
                    inputs.put("question", q.getContent());
                    inputs.put("standard_answer", q.getAnswer());
                    inputs.put("student_answer", detail.getStudentAnswer());
                    inputs.put("max_score", detail.getMaxScore());

                    try {
                        // 调用 Workflow
                        Map<String, Object> result = difyClient.runWorkflow(apiKey, inputs, record.getUserId().toString());
                        
                        if (result != null && result.containsKey("data")) {
                            Map data = (Map) result.get("data");
                            Map outputs = (Map) data.get("outputs");
                            
                            // 解析 Dify 返回
                            String outputJson = (String) outputs.get("text"); 
                            outputJson = cleanJsonString(outputJson);

                            JSONObject aiRes = JSONUtil.parseObj(outputJson);
                            BigDecimal aiScore = aiRes.getBigDecimal("score", BigDecimal.ZERO);
                            String aiComment = aiRes.getStr("comment", "AI 阅卷完成");

                            // 校验分数边界
                            if (aiScore.compareTo(detail.getMaxScore()) > 0) aiScore = detail.getMaxScore();
                            if (aiScore.compareTo(BigDecimal.ZERO) < 0) aiScore = BigDecimal.ZERO;

                            // 更新明细
                            detail.setScore(aiScore);
                            detail.setAiComment(aiComment);
                            detail.setIsMarked((byte) 1);
                            detail.setIsCorrect(aiScore.compareTo(detail.getMaxScore()) == 0 ? (byte)1 : (byte)0);
                            
                            hasUpdates = true;
                            log.info("题目[{}] AI 评分完成: 得分={}", q.getId(), aiScore);

                            // 如果得分低于满分的 60%，加入错题本
                            BigDecimal passLine = detail.getMaxScore().multiply(new BigDecimal("0.6"));
                            if (aiScore.compareTo(passLine) < 0) {
                                MistakeBook mb = new MistakeBook();
                                mb.setUserId(record.getUserId());
                                mb.setQuestionId(q.getId());
                                mb.setCourseId(q.getCourseId());
                                mb.setLastWrongAnswer(detail.getStudentAnswer());
                                mb.setCreateBy(record.getUserId());
                                mb.setUpdateBy(record.getUserId());
                                mb.setCreateTime(LocalDateTime.now());
                                mb.setUpdateTime(LocalDateTime.now());
                                newMistakes.add(mb);
                            }
                        }
                    } catch (Exception e) {
                        log.error("题目[{}] Dify 调用失败", q.getId(), e);
                    }
                }
                
                // 累加主观题分数
                if (detail.getIsMarked() == 1) {
                    totalScore = totalScore.add(detail.getScore());
                }
            }

            // 4. 更新数据库
            if (hasUpdates) {
                recordDetailService.updateBatchById(details);
                
                // 更新主记录 (改用 Mapper 更新)
                record.setTotalScore(totalScore);
                boolean allMarked = details.stream().allMatch(d -> d.getIsMarked() == 1);
                record.setStatus(allMarked ? (byte) 3 : (byte) 2); // 3-已完成批阅
                record.setUpdateTime(LocalDateTime.now());
                recordMapper.updateById(record);

                // 保存错题 (保持原样)
                if (!newMistakes.isEmpty()) {
                    for (MistakeBook mb : newMistakes) {
                        long exists = mistakeBookService.count(new LambdaQueryWrapper<MistakeBook>()
                                .eq(MistakeBook::getUserId, mb.getUserId())
                                .eq(MistakeBook::getQuestionId, mb.getQuestionId()));
                        if (exists == 0) {
                            mistakeBookService.save(mb);
                        } else {
                            MistakeBook existing = mistakeBookService.getOne(new LambdaQueryWrapper<MistakeBook>()
                                    .eq(MistakeBook::getUserId, mb.getUserId())
                                    .eq(MistakeBook::getQuestionId, mb.getQuestionId()));
                            existing.setWrongCount(existing.getWrongCount() + 1);
                            existing.setLastWrongAnswer(mb.getLastWrongAnswer());
                            existing.setUpdateTime(LocalDateTime.now());
                            mistakeBookService.updateById(existing);
                        }
                    }
                }
            }

            log.info("<<< AI 阅卷结束: recordId={}, NewTotal={}", recordId, totalScore);

        } catch (Exception e) {
            log.error("AI 阅卷流程异常", e);
        }
    }

    private String cleanJsonString(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) return "{}";
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substring(7);
        }
        if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substring(3);
        }
        if (jsonStr.endsWith("```")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
        }
        return jsonStr.trim();
    }
}
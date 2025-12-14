package com.university.exam.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.utils.DifyClient;
import com.university.exam.entity.MistakeBook;
import com.university.exam.entity.Question;
import com.university.exam.entity.Record;
import com.university.exam.entity.RecordDetail;
import com.university.exam.mapper.RecordMapper;
import com.university.exam.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 2025-12-14:
 * 1. 注入 RecordMapper 解决循环依赖。
 * 2. 增加获取 RecordDetail 的重试机制。
 * 3. 增加 @Transactional 事务注解。
 * 4. 显式更新 updateTime，并只更新有变动的明细记录。
 * 5. 修改状态流转逻辑：AI 阅卷后状态强制保持为 2（待复核）。
 * 6. 核心逻辑调整：AI 评分后 is_marked 保持为 0，必须由人工复核后才能标记为已阅。
 * 7. 确保 total_score 正确累加客观题分数。
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
    private final RecordMapper recordMapper;
    private final RecordDetailService recordDetailService;
    private final QuestionService questionService;
    private final MistakeBookService mistakeBookService;

    @Override
    @Async("aiTaskExecutor")
    @Transactional(rollbackFor = Exception.class) // 增加事务控制，确保原子性
    public void gradeSubjectiveQuestionsAsync(Long recordId) {
        log.info(">>> 开始 AI 自动阅卷: recordId={}", recordId);

        try {
            // 1. 获取必要的上下文
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

            // 2. 获取答题明细 (增加重试机制)
            List<RecordDetail> details = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                    .eq(RecordDetail::getRecordId, recordId));

            // 防御性重试：如果列表为空，暂停 500ms 后再试一次
            if (details.isEmpty()) {
                log.warn("RecordDetail 为空，尝试重试等待...");
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                details = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                        .eq(RecordDetail::getRecordId, recordId));
            }

            if (details.isEmpty()) {
                log.error("无法获取答题明细，阅卷终止: recordId={}", recordId);
                return;
            }

            BigDecimal totalScore = BigDecimal.ZERO; // 这里的 totalScore 将作为最终写入 record 的总分 (目前仅包含客观题)
            List<MistakeBook> newMistakes = new ArrayList<>();
            List<RecordDetail> updates = new ArrayList<>(); // 只存储需要更新的记录

            for (RecordDetail detail : details) {
                Question q = questionService.getById(detail.getQuestionId());
                if (q == null) continue;

                // --- 累加已有的客观题分数 (关键逻辑) ---
                if (q.getType() == 1 || q.getType() == 2 || q.getType() == 3) {
                    // 1-单选, 2-多选, 3-判断
                    // 只要标记为已阅且有分数，就累加
                    if (detail.getIsMarked() != null && detail.getIsMarked() == 1) {
                        BigDecimal score = detail.getScore() != null ? detail.getScore() : BigDecimal.ZERO;
                        totalScore = totalScore.add(score);
                        log.debug("客观题[{}] 计入总分: {}", q.getId(), score);
                    }
                    continue;
                }

                // 3. 筛选主观题 (简答-4, 填空-5) 且未批改的进行 AI 评分
                if ((q.getType() == 4 || q.getType() == 5) && (detail.getIsMarked() == null || detail.getIsMarked() == 0)) {
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

                            // --- 核心逻辑 ---
                            // 1. 设置 AI 建议分数
                            detail.setScore(aiScore);
                            // 2. 设置 AI 评语
                            detail.setAiComment(aiComment);
                            // 3. 保持 is_marked 为 0（未批改），等待人工确认
                            detail.setIsMarked((byte) 0); 
                            // 4. 设置预判正确性 (仅供参考)
                            detail.setIsCorrect(aiScore.compareTo(detail.getMaxScore()) == 0 ? (byte) 1 : (byte) 0);
                            
                            // 显式更新时间
                            detail.setUpdateTime(LocalDateTime.now());
                            
                            updates.add(detail); // 加入待更新列表
                            log.info("题目[{}] AI 预评分完成: 建议得分={}, ai评语={}", q.getId(), aiScore, aiComment);

                            // 错题本逻辑 (AI 预判错题)
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

                // 注意：主观题由于 isMarked=0，此处不会累加到 totalScore
                // 这是符合预期的，因为必须人工复核后才算分。
            }

            // 4. 更新数据库
            if (CollUtil.isNotEmpty(updates)) {
                log.info("正在批量更新 {} 条阅卷结果 (AI预填)...", updates.size());
                recordDetailService.updateBatchById(updates);
            }

            // 更新主记录
            log.info("更新 Record 总分 (仅含客观题): {}", totalScore);
            record.setTotalScore(totalScore); 
            record.setStatus((byte) 2); // 状态保持为 2 (待复核)
            record.setUpdateTime(LocalDateTime.now());
            recordMapper.updateById(record);
            
            // 保存错题
            if (!newMistakes.isEmpty()) {
                saveMistakes(newMistakes);
            }

            log.info("<<< AI 阅卷结束: recordId={}, CurrentTotal={} (等待人工复核生效)", recordId, totalScore);

        } catch (Exception e) {
            log.error("AI 阅卷流程异常", e);
            // 事务回滚
            throw new RuntimeException(e);
        }
    }

    private void saveMistakes(List<MistakeBook> newMistakes) {
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
package com.university.exam.common.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.dto.QuestionExcelDto;
import com.university.exam.entity.Question;
import com.university.exam.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目导入监听器
 *
 * @author MySQL数据库架构师
 * @version 1.1.0 (支持用户友好格式解析)
 * @since 2025-12-10
 */
@Slf4j
public class QuestionExcelListener implements ReadListener<QuestionExcelDto> {

    /**
     * 每隔100条存储数据库，实际使用中可以调整
     */
    private static final int BATCH_COUNT = 100;
    private final List<Question> cachedDataList = new ArrayList<>(BATCH_COUNT);
    private final QuestionService questionService;
    private final Long courseId;
    private final Long userId;

    public QuestionExcelListener(QuestionService questionService, Long courseId, Long userId) {
        this.questionService = questionService;
        this.courseId = courseId;
        this.userId = userId;
    }

    @Override
    public void invoke(QuestionExcelDto data, AnalysisContext context) {
        if (!StringUtils.hasText(data.getContent())) {
            return;
        }

        Question question = new Question();
        question.setCourseId(courseId);
        question.setContent(data.getContent());
        // 计算 Hash 用于查重
        question.setContentHash(DigestUtil.md5Hex(data.getContent()));

        Byte type = data.getType() != null ? data.getType().byteValue() : 1;
        question.setType(type);
        question.setDifficulty(data.getDifficulty() != null ? data.getDifficulty().byteValue() : 1);
        question.setAnalysis(data.getAnalysis());

        // 1. 处理选项 (支持 JSON 或 | 分隔)
        String optionsStr = data.getOptions();
        if (StringUtils.hasText(optionsStr)) {
            // 如果不是 JSON 数组格式（不以 [ 开头），则按 | 分隔处理
            if (!optionsStr.trim().startsWith("[")) {
                String[] opts = optionsStr.split("[|｜]"); // 支持中文和英文管道符
                List<String> optList = new ArrayList<>();
                for (String opt : opts) {
                    optList.add(opt.trim());
                }
                question.setOptions(JSONUtil.toJsonStr(optList));
            } else {
                question.setOptions(optionsStr);
            }
        }

        // 2. 处理答案 (支持 A,B,C 转 0,1,2)
        String ansStr = data.getAnswer();
        if (StringUtils.hasText(ansStr)) {
            ansStr = ansStr.trim();
            
            if (type == 3) { // 判断题
                if ("正确".equals(ansStr) || "对".equals(ansStr) || "Yes".equalsIgnoreCase(ansStr) || "1".equals(ansStr)) {
                    question.setAnswer("1");
                } else {
                    question.setAnswer("0");
                }
            } else if (type == 1 || type == 2) { // 单选或多选
                // 如果不是 JSON 格式，尝试解析字母
                if (!ansStr.startsWith("[")) {
                    List<Integer> indices = new ArrayList<>();
                    // 分割 A, B, C
                    String[] parts = ansStr.split("[,，]"); // 支持中英文逗号
                    for (String part : parts) {
                        part = part.trim().toUpperCase();
                        if (part.matches("[A-Z]")) {
                            indices.add(part.charAt(0) - 'A');
                        } else if (part.matches("\\d+")) {
                            // 如果用户直接填数字，兼容处理 (假设用户填 0 或 1)
                            indices.add(Integer.parseInt(part));
                        }
                    }
                    
                    if (type == 1) { // 单选，直接存 "0", "1"
                        if (!indices.isEmpty()) question.setAnswer(indices.get(0).toString());
                    } else { // 多选，存 JSON "[0,1]"
                        indices.sort(Integer::compareTo);
                        question.setAnswer(JSONUtil.toJsonStr(indices));
                    }
                } else {
                    question.setAnswer(ansStr);
                }
            } else {
                // 简答/填空
                question.setAnswer(ansStr);
            }
        }

        // 3. 处理标签 (支持逗号分隔)
        String tagsStr = data.getTags();
        if (StringUtils.hasText(tagsStr)) {
            if (!tagsStr.trim().startsWith("[")) {
                String[] tags = tagsStr.split("[,，]");
                List<String> tagList = new ArrayList<>();
                for (String tag : tags) {
                    tagList.add(tag.trim());
                }
                question.setTags(JSONUtil.toJsonStr(tagList));
            } else {
                question.setTags(tagsStr);
            }
        }

        question.setCreateBy(userId);
        question.setUpdateBy(userId);
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());

        cachedDataList.add(question);

        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            cachedDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info("所有数据解析完成！");
    }

    private void saveData() {
        if (cachedDataList.isEmpty()) {
            return;
        }
        log.info("{}条数据，开始存储数据库！", cachedDataList.size());

        // 简单查重逻辑
        List<Question> toInsert = new ArrayList<>();
        for (Question q : cachedDataList) {
            long exists = questionService.count(new LambdaQueryWrapper<Question>()
                    .eq(Question::getContentHash, q.getContentHash()));
            if (exists == 0) {
                toInsert.add(q);
            }
        }

        if (!toInsert.isEmpty()) {
            questionService.saveBatch(toInsert);
        }
        log.info("存储数据库成功！实际入库 {} 条", toInsert.size());
    }
}
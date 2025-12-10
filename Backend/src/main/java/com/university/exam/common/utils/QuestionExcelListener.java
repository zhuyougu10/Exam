package com.university.exam.common.utils;

import cn.hutool.crypto.digest.DigestUtil;
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
 * @version 1.0.0
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

        question.setType(data.getType() != null ? data.getType().byteValue() : 1);
        question.setDifficulty(data.getDifficulty() != null ? data.getDifficulty().byteValue() : 1);
        question.setOptions(data.getOptions());
        question.setAnswer(data.getAnswer());
        question.setAnalysis(data.getAnalysis());
        question.setTags(data.getTags());

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

        // 简单查重逻辑：如果库里已有Hash，则跳过不插入（或者选择更新，这里选择跳过）
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
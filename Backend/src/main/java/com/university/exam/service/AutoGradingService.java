package com.university.exam.service;

import java.util.concurrent.CompletableFuture;

/**
 * AI 自动阅卷服务接口
 * 负责调用 Dify 工作流对主观题进行评分
 *
 * @author MySQL数据库架构师
 * @since 2025-12-14
 */
public interface AutoGradingService {

    /**
     * 异步执行单份试卷的 AI 阅卷任务
     * 通常在考生交卷后，或者由定时任务触发
     *
     * @param recordId 考试记录ID
     */
    void gradeSubjectiveQuestionsAsync(Long recordId);
}
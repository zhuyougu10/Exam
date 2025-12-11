package com.university.exam.service;

import java.util.List;

/**
 * 智能出题服务接口
 *
 * @author MySQL数据库架构师
 * @version 1.2.0 (支持多选类型)
 * @since 2025-12-10
 */
public interface QuestionGenerationService {

    /**
     * 启动异步出题任务
     *
     * @param courseId   课程ID
     * @param topic      出题主题/知识点
     * @param totalCount 需要生成的题目总数
     * @param difficulty 难度 (1-简单, 2-中等, 3-困难)
     * @param types      题目类型列表 (1-单选, 2-多选, 3-判断, 4-简答, 5-填空)
     * @param userId     操作用户ID
     * @return 任务ID (sys_ai_task表的主键)
     */
    Long startGenerationTask(Long courseId, String topic, int totalCount, String difficulty, List<Integer> types, Long userId);

    /**
     * 实际执行异步生成逻辑的方法
     */
    void processGenerationAsync(Long taskId, Long courseId, String topic, int totalCount, String difficulty, List<Integer> types, Long userId, String apiKey);
}
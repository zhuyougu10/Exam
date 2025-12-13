package com.university.exam.service;

import com.university.exam.common.dto.student.DashboardStatsVo;
import com.university.exam.common.dto.student.ExamPaperVo;
import com.university.exam.common.dto.student.SubmitExamRequest;
import com.university.exam.entity.Record;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 考试记录表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface RecordService extends IService<Record> {

    /**
     * 开始考试（创建记录或断点续考）
     *
     * @param userId    用户ID
     * @param publishId 发布ID
     * @return 脱敏后的试卷信息
     */
    ExamPaperVo startExam(Long userId, Long publishId);

    /**
     * 提交试卷
     *
     * @param request 交卷请求
     * @param userId  用户ID
     */
    void submitExam(SubmitExamRequest request, Long userId);

    /**
     * 获取学生仪表盘统计
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    DashboardStatsVo getStudentStats(Long userId);
}
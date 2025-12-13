package com.university.exam.common.dto.review;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阅卷列表响应 VO
 *
 * @author MySQL数据库架构师
 * @since 2025-12-14
 */
@Data
public class ReviewListResp {
    private Long id;            // 考试记录ID
    private Long userId;        // 学生ID
    private String studentName; // 学生姓名
    private String deptName;    // 班级名称
    
    private Long paperId;
    private String paperTitle;  // 试卷标题
    
    private BigDecimal totalScore; // 当前总分
    private Byte status;        // 状态: 2-待批改, 3-已完成
    
    private LocalDateTime submitTime; // 交卷时间
    
    // 进度 (例如 "85%" 或 "待人工复核")
    private String reviewStatusDesc;
}
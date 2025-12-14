package com.university.exam.common.dto.review;

import lombok.Data;

/**
 * 阅卷控制台-试卷列表响应 VO
 * 包含试卷基本信息及阅卷进度统计
 *
 * @author MySQL数据库架构师
 * @since 2025-12-14
 */
@Data
public class ReviewPaperResp {
    private Long id;            // 试卷ID
    private String title;       // 试卷标题
    private Long courseId;      // 课程ID
    private String courseName;  // 课程名称
    
    private Integer status;     // 试卷状态
    
    private Long pendingCount;  // 待批改数量 (status=2)
    private Long reviewedCount; // 已完成数量 (status=3)
    private Long totalCount;    // 总提交数量
}
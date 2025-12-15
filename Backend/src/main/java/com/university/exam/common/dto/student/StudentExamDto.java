package com.university.exam.common.dto.student;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentExamDto {
    private Long id;
    
    // 关联的试卷ID
    private Long paperId;
    
    // 考试名称
    private String title;
    
    // 考试时长（分钟）
    private Integer duration;
    
    // 考试开始时间
    private LocalDateTime startTime;
    
    // 考试结束时间
    private LocalDateTime endTime;
    
    // 考试状态：0-未开始，1-进行中，2-已结束
    private Integer status;
    
    // 考试记录ID（如果有）
    private Long recordId;
    
    // 考试发布ID
    private Long publishId;

    // 是否需要密码
    private Boolean needPassword;
    
    // 限制次数
    private Integer limitCount;
    
    // 已尝试次数
    private Integer triedCount;
}
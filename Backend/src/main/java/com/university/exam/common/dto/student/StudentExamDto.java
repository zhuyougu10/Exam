package com.university.exam.common.dto.student;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考生端-考试列表项 DTO
 */
@Data
public class StudentExamDto {
    private Long id;            // 考试发布ID (Publish ID)
    private String title;       // 考试标题
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;   // 考试时长(分钟)
    private Integer limitCount; // 限考次数
    private Integer triedCount; // 已考次数
    private Integer status;     // 状态：0-未开始, 1-进行中, 2-已结束/已交卷
    private Long paperId;       // 试卷ID (部分场景可能需要)
}
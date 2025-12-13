package com.university.exam.common.dto.student;

import lombok.Data;
import java.util.List;

/**
 * 考生交卷请求 DTO
 */
@Data
public class SubmitExamRequest {
    private Long recordId; // 考试记录ID
    private List<AnswerItem> answers; // 答题卡

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer; // 用户答案
    }
}
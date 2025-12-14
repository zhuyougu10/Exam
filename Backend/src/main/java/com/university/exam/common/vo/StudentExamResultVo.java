package com.university.exam.common.vo;

import lombok.Data;
import java.util.List;

@Data
public class StudentExamResultVo {
    // 试卷头部信息
    private String title;
    private Double userScore;
    private Double totalScore;
    private Double passScore;
    private String startTime;
    private String submitTime;
    private String duration; 

    // 结果详情列表
    private List<QuestionResultItem> questionList;

    @Data
    public static class QuestionResultItem {
        private Long id;
        private Integer questionNo;
        private Integer type;
        private String content;
        private String options;
        private String imageUrl;
        
        private String studentAnswer;
        private String correctAnswer;
        private String analysis;
        
        private Double score;
        private Double maxScore;
        private Integer isCorrect;
        private String comment;
    }
}
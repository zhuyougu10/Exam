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
    private String duration; // 格式化后的时长，如 "45分钟"
    
    // 结果详情列表
    private List<QuestionResultItem> questionList;

    @Data
    public static class QuestionResultItem {
        private Long id;          // 题目ID
        private Integer type;     // 题型
        private String content;   // 题干
        private String options;   // 选项 JSON 字符串
        private String imageUrl;  // 图片
        
        private String studentAnswer; // 学生作答
        private String correctAnswer; // 参考答案
        private String analysis;      // 解析
        
        private Double score;     // 学生得分
        private Double maxScore;  // 本题满分
        private Integer isCorrect; // 是否正确 (1-对, 0-错)
        private String comment;    // 评语 (AI或教师)
    }
}
package com.university.exam.common.dto.student;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 考生端-试卷视图 VO (脱敏后)
 */
@Data
public class ExamPaperVo {
    private Long recordId;      // 考试记录ID (用于交卷)
    private Long paperId;       // 试卷ID
    private String title;       // 试卷标题
    private BigDecimal totalScore; // 总分
    private Integer duration;   // 考试时长(分钟)
    private Long remainingSeconds; // 剩余时间(秒) - 用于倒计时
    private List<QuestionVo> questions; // 题目列表

    @Data
    public static class QuestionVo {
        private Long id;            // 题目ID
        private Integer type;       // 题型
        private String content;     // 题干
        private String imageUrl;    // 图片
        private String options;     // 选项(JSON字符串)
        private BigDecimal score;   // 分值
        private String savedAnswer; // 已保存的答案 (断点续考用)
        // 注意：严格排除 answer 和 analysis 字段
    }
}
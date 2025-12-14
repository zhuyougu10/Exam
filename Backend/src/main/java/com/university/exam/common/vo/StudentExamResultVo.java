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

    // --- 补全缺失字段，解决 RecordServiceImpl 编译错误 ---
    private Double beatRate;           // 击败考生比例
    private List<RadarItem> radarData; // 能力雷达图数据
    // --------------------------------------------------

    // 新增字段：控制前端展示
    private Boolean canViewAnalysis; // 是否允许查看详情（若为false，questionList将为空）
    private String examEndTime;      // 考试截止时间（用于提示）

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

    // --- 补全缺失内部类 ---
    @Data
    public static class RadarItem {
        private String name;
        private Double value;
        private Double max;
        
        public RadarItem() {}
        
        public RadarItem(String name, Double value, Double max) {
            this.name = name;
            this.value = value;
            this.max = max;
        }
    }
}
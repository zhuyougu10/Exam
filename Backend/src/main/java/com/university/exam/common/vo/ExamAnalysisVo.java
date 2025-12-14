package com.university.exam.common.vo;

import lombok.Data;
import java.util.List;

@Data
public class ExamAnalysisVo {
    // 核心指标
    private Double avgScore;    // 平均分
    private Double maxScore;    // 最高分
    private Double minScore;    // 最低分
    private Double passRate;    // 及格率
    private Integer attendeeCount; // 参考人数

    // 图表数据：分数段分布
    private List<String> scoreRanges; // ["0-60", "60-70"...]
    private List<Integer> scoreCounts; // [5, 10...]

    // 图表数据：知识点掌握率 (雷达图)
    private List<RadarItem> knowledgeRadar;

    // 错题排行
    private List<WrongQuestionItem> wrongTop10;

    @Data
    public static class RadarItem {
        private String name;  // 知识点名称
        private Double value; // 掌握率 (0-100)
        private Double max;   // 满分 (通常100)
    }

    @Data
    public static class WrongQuestionItem {
        private Long id;
        private String content;
        private String type; // 单选/多选
        private String knowledge;
        private Double errorRate; // 错误率
    }
}
package com.university.exam.common.vo;

import lombok.Data;
import java.util.List;

@Data
public class StudentTrendVo {
    private List<String> examNames;  // 考试名称轴
    private List<Double> scores;     // 分数轴
    private String suggestion;       // 学习建议
}
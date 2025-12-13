package com.university.exam.common.dto.student;

import lombok.Data;

/**
 * 考生仪表盘统计数据 VO
 */
@Data
public class DashboardStatsVo {
    private Integer examCount;      // 参加考试场次
    private Double avgScore;        // 平均分
    private Integer passCount;      // 及格次数
    private Integer mistakeCount;   // 错题本题目总数
    private Integer upcomingCount;  // 待参加考试数
}
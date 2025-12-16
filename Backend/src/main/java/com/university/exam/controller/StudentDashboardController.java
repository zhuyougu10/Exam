package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.dto.student.DashboardStatsVo;

import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.common.vo.StudentTrendVo;
import com.university.exam.entity.MistakeBook;
import com.university.exam.entity.Publish;
import com.university.exam.entity.Record;
import com.university.exam.service.MistakeBookService;
import com.university.exam.service.PublishService;
import com.university.exam.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/dashboard")
public class StudentDashboardController {

    @Autowired
    private RecordService recordService;
    @Autowired
    private PublishService publishService;
    @Autowired
    private MistakeBookService mistakeBookService;
    @Autowired
    private JwtUtils jwtUtils;

    // 获取核心统计
    @GetMapping("/stats")
    public Result<DashboardStatsVo> getStats(@RequestHeader("Authorization") String token) {
        // Fix: Remove "Bearer " prefix and whitespace if present
        String actualToken = token.replace("Bearer ", "").trim();
        Long userId = jwtUtils.getUserIdFromToken(actualToken);
        DashboardStatsVo vo = new DashboardStatsVo();

        // 1. 考试场次
        long examCount = recordService.count(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .ge(Record::getStatus, 1)); // 1=已开始但未交, 2=已交...
        vo.setExamCount((int) examCount);

        // 2. 错题数量
        long mistakeCount = mistakeBookService.count(new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId));
        vo.setMistakeCount((int) mistakeCount);

        // 3. 计算平均分
        List<Record> finishedRecords = recordService.list(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .ge(Record::getStatus, 2)); // 2=已交卷

        if (!finishedRecords.isEmpty()) {
            double totalScore = finishedRecords.stream()
                    .mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0.0)
                    .sum();
            double avg = totalScore / finishedRecords.size();
            vo.setAvgScore(Math.round(avg * 10.0) / 10.0);
        } else {
            vo.setAvgScore(0.0);
        }
        
        // 4. 即将开始的考试：查询尚未结束且学生未完成的考试数量
        LocalDateTime now = LocalDateTime.now();
        // 查询所有尚未结束且已启用的考试
        List<Publish> activePublishes = publishService.list(new LambdaQueryWrapper<Publish>()
                .eq(Publish::getStatus, 1)
                .gt(Publish::getEndTime, now));
        
        // 获取该学生已完成的考试发布ID集合
        Set<Long> completedPublishIds = recordService.list(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .ge(Record::getStatus, 2))
                .stream()
                .map(Record::getPublishId)
                .collect(Collectors.toSet());
        
        // 计算未完成的考试数量
        long upcomingCount = activePublishes.stream()
                .filter(p -> !completedPublishIds.contains(p.getId()))
                .count();
        vo.setUpcomingCount((int) upcomingCount);

        return Result.success(vo);
    }

    // 新增：获取成绩趋势
    @GetMapping("/trend")
    public Result<StudentTrendVo> getTrend(@RequestHeader("Authorization") String token) {
        // Fix: Remove "Bearer " prefix and whitespace if present
        String actualToken = token.replace("Bearer ", "").trim();
        Long userId = jwtUtils.getUserIdFromToken(actualToken);
        StudentTrendVo vo = new StudentTrendVo();

        // 获取最近 7 次考试成绩
        List<Record> recentRecords = recordService.list(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .ge(Record::getStatus, 2) // 已交卷
                .orderByAsc(Record::getSubmitTime) // 按时间正序
                .last("LIMIT 7"));

        List<String> names = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        for (Record r : recentRecords) {
            Publish p = publishService.getById(r.getPublishId());
            // 截取标题前6个字，避免图表拥挤
            String title = p != null ? p.getTitle() : "未知考试";
            if (title.length() > 6) title = title.substring(0, 6) + "..";
            names.add(title);
            // 修复: BigDecimal转Double
            scores.add(r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0.0);
        }

        vo.setExamNames(names);
        vo.setScores(scores);

        // 生成简单建议
        if (scores.isEmpty()) {
            vo.setSuggestion("暂无考试数据，快去参加一场考试吧！");
        } else {
            double lastScore = scores.get(scores.size() - 1);
            if (lastScore < 60) {
                vo.setSuggestion("最近一次考试成绩不理想，建议通过错题本重点复习薄弱知识点。");
            } else if (lastScore > 90) {
                vo.setSuggestion("表现优秀！请继续保持良好的学习状态，尝试挑战更高难度的题目。");
            } else {
                vo.setSuggestion("成绩稳步提升中，建议针对丢分项进行专项训练。");
            }
        }

        return Result.success(vo);
    }
}
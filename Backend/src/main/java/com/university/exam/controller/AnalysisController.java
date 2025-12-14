package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.university.exam.common.result.Result;
import com.university.exam.entity.*;
import com.university.exam.entity.Record;
import com.university.exam.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final UserService userService;
    private final PaperService paperService;
    private final QuestionService questionService;
    private final RecordService recordService;
    private final RecordDetailService recordDetailService;

    /**
     * 获取当前用户ID (替代 JwtUtils.getUserId)
     */
    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            // 假设 Principal 存储的是 ID 的字符串形式，如果存的是用户名则需要查库
            return Long.valueOf(principal.toString());
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return 0L;
        }
    }

    /**
     * 学生端：个人知识点雷达图数据
     * 逻辑：获取学生所有考试记录 -> 提取答题详情 -> 关联题目知识点 -> 计算得分率
     */
    @GetMapping("/analysis/student/radar")
    public Result<List<Map<String, Object>>> getStudentRadar() {
        Long userId = getCurrentUserId();

        // 1. 获取该学生的所有考试记录ID
        List<Record> records = recordService.list(new QueryWrapper<Record>().eq("student_id", userId));
        if (records.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Long> recordIds = records.stream().map(Record::getId).collect(Collectors.toList());

        // 2. 获取所有答题详情
        List<RecordDetail> details = recordDetailService.list(new QueryWrapper<RecordDetail>().in("record_id", recordIds));
        if (details.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 3. 获取涉及的题目信息
        Set<Long> questionIds = details.stream().map(RecordDetail::getQuestionId).collect(Collectors.toSet());
        if (questionIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Question> questions = questionService.listByIds(questionIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        // 4. 聚合计算 (Key: 知识点/Tag, Value: [总得分, 总分])
        Map<String, double[]> statsMap = new HashMap<>();

        for (RecordDetail detail : details) {
            Question q = questionMap.get(detail.getQuestionId());
            if (q == null) continue;

            // 使用 tags 作为知识点标识，如果为 null 则使用 "综合知识"
            String tag = (q.getTags() != null && !q.getTags().isEmpty()) ? q.getTags().split(",")[0] : "综合知识";
            
            // 累计 (处理 BigDecimal 转换)
            BigDecimal scoreBd = detail.getScore(); // 修正: getStudentScore -> getScore
            double earnedScore = scoreBd != null ? scoreBd.doubleValue() : 0.0;
            
            // 修正: Question 实体通常没有 score 字段(分数在试卷中)，这里暂用 10.0 作为默认满分基准，或根据难度加权
            // 如果 Question 实体确实有 score 字段且是 BigDecimal，请使用 q.getScore().doubleValue()
            double maxScore = 10.0; 

            statsMap.computeIfAbsent(tag, k -> new double[]{0.0, 0.0});
            statsMap.get(tag)[0] += earnedScore;
            statsMap.get(tag)[1] += maxScore;
        }

        // 5. 格式化输出
        List<Map<String, Object>> result = new ArrayList<>();
        statsMap.forEach((key, val) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", key);
            // 计算得分率 (0-100)
            double rate = val[1] > 0 ? (val[0] / val[1]) * 100 : 0;
            item.put("value", BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP));
            item.put("max", 100);
            result.add(item);
        });

        // 取前6个主要知识点，避免雷达图太乱
        return Result.success(result.stream().limit(6).collect(Collectors.toList()));
    }

    /**
     * 教师端：仪表盘基础统计数据
     */
    @GetMapping("/teacher/dashboard/stats")
    public Result<Map<String, Object>> getTeacherDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 学生总数 (Role = 2 假设为学生)
        long studentCount = userService.count(new QueryWrapper<User>().eq("role", 2));
        
        // 2. 试卷总数
        long paperCount = paperService.count();

        // 3. 题库题目总数
        long questionCount = questionService.count();

        // 4. 累计考试人次
        long examRecordCount = recordService.count();

        stats.put("studentCount", studentCount);
        stats.put("paperCount", paperCount);
        stats.put("questionCount", questionCount);
        stats.put("examRecordCount", examRecordCount);

        return Result.success(stats);
    }

    /**
     * 教师端：单场考试/试卷详细分析
     */
    @GetMapping("/teacher/exam/analysis")
    public Result<Map<String, Object>> getExamAnalysis(@RequestParam("paperId") Long paperId) {
        // 1. 获取该试卷的所有考试记录
        List<Record> records = recordService.list(new QueryWrapper<Record>().eq("paper_id", paperId));

        Map<String, Object> analysis = new HashMap<>();
        
        if (records.isEmpty()) {
            analysis.put("averageScore", 0);
            analysis.put("maxScore", 0);
            analysis.put("minScore", 0);
            analysis.put("attendeeCount", 0);
            analysis.put("passRate", 0);
            analysis.put("distribution", Collections.emptyMap());
            return Result.success(analysis);
        }

        // 2. 基础统计 (处理 BigDecimal 转换)
        DoubleSummaryStatistics scoreStats = records.stream()
                .mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0.0)
                .summaryStatistics();

        analysis.put("averageScore", BigDecimal.valueOf(scoreStats.getAverage()).setScale(1, RoundingMode.HALF_UP));
        analysis.put("maxScore", scoreStats.getMax());
        analysis.put("minScore", scoreStats.getMin());
        analysis.put("attendeeCount", scoreStats.getCount());

        // 3. 及格率 (>= 60% 总分)
        Paper paper = paperService.getById(paperId);
        // 处理 Paper 中可能也是 BigDecimal 的情况
        double totalMark = 100.0;
        if (paper != null && paper.getTotalScore() != null) {
            // 假设 getTotalScore 返回 Integer 或 BigDecimal
            totalMark = paper.getTotalScore().doubleValue(); 
        }
        
        double passMark = totalMark * 0.6;

        long passCount = records.stream()
                .filter(r -> (r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0) >= passMark)
                .count();
        double passRate = (double) passCount / records.size() * 100;
        analysis.put("passRate", BigDecimal.valueOf(passRate).setScale(1, RoundingMode.HALF_UP));

        // 4. 分数段分布
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-60%", 0L);
        distribution.put("60-70%", 0L);
        distribution.put("70-80%", 0L);
        distribution.put("80-90%", 0L);
        distribution.put("90-100%", 0L);

        for (Record r : records) {
            double s = r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0;
            double ratio = totalMark > 0 ? s / totalMark : 0;
            
            if (ratio < 0.6) distribution.put("0-60%", distribution.get("0-60%") + 1);
            else if (ratio < 0.7) distribution.put("60-70%", distribution.get("60-70%") + 1);
            else if (ratio < 0.8) distribution.put("70-80%", distribution.get("70-80%") + 1);
            else if (ratio < 0.9) distribution.put("80-90%", distribution.get("80-90%") + 1);
            else distribution.put("90-100%", distribution.get("90-100%") + 1);
        }
        analysis.put("distribution", distribution);

        return Result.success(analysis);
    }
}
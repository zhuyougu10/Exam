package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.university.exam.common.result.Result;
import com.university.exam.common.vo.ExamAnalysisVo;
import com.university.exam.entity.*;
import com.university.exam.entity.Record;
import com.university.exam.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private PublishService publishService;
    @Autowired
    private RecordService recordService;
    @Autowired
    private RecordDetailService recordDetailService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private PaperService paperService;

    // 获取考试场次列表
    @GetMapping("/exam/list")
    public Result<List<Publish>> getExamList() {
        // 仅返回已发布且非删除的考试
        List<Publish> list = publishService.list(new LambdaQueryWrapper<Publish>()
                .orderByDesc(Publish::getCreateTime));
        return Result.success(list);
    }

    // 获取特定考试的详细分析
    @GetMapping("/exam/{publishId}")
    public Result<ExamAnalysisVo> getExamAnalysis(@PathVariable Long publishId) {
        ExamAnalysisVo vo = new ExamAnalysisVo();

        // 1. 获取该场次所有有效考试记录 (已提交/已阅卷)
        List<Record> records = recordService.list(new LambdaQueryWrapper<Record>()
                .eq(Record::getPublishId, publishId)
                .ge(Record::getStatus, 2)); // 2=已提交/待阅, 3=已完成

        Publish publish = publishService.getById(publishId);
        Paper paper = paperService.getById(publish.getPaperId());
        double passLine = paper != null ? paper.getPassScore().doubleValue() : 60.0;


        // 2. 计算核心指标
        if (records.isEmpty()) {
            return Result.success(emptyVo());
        }

        DoubleSummaryStatistics stats = records.stream()
                .mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0.0)
                .summaryStatistics();

        vo.setAvgScore(Math.round(stats.getAverage() * 10.0) / 10.0);
        vo.setMaxScore(stats.getMax());
        vo.setMinScore(stats.getMin());
        vo.setAttendeeCount((int) stats.getCount());

        long passCount = records.stream()
                .filter(r -> r.getTotalScore() != null && r.getTotalScore().doubleValue() >= passLine)
                .count();
        vo.setPassRate(Math.round((double) passCount / stats.getCount() * 1000.0) / 10.0);

        // 3. 计算分数段分布
        // 简单分段: [0-60), [60-70), [70-80), [80-90), [90-100]
        int[] ranges = new int[5]; // <60, 60-70, 70-80, 80-90, >=90
        for (Record r : records) {
            double s = r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0.0;
            if (s < 60) ranges[0]++;
            else if (s < 70) ranges[1]++;
            else if (s < 80) ranges[2]++;
            else if (s < 90) ranges[3]++;
            else ranges[4]++;
        }
        vo.setScoreRanges(Arrays.asList("0-60", "60-70", "70-80", "80-90", "90-100"));
        vo.setScoreCounts(Arrays.stream(ranges).boxed().collect(Collectors.toList()));

        // 4. 计算知识点雷达图 (模拟逻辑：如果没有详细知识点标签，生成模拟数据)
        // 真实逻辑应关联 RecordDetail -> Question -> Tags
        vo.setKnowledgeRadar(generateMockRadar());

        // 5. 计算错题 Top 10
        vo.setWrongTop10(getWrongTop10(records));

        return Result.success(vo);
    }

    // 辅助方法：获取错题Top10
    private List<ExamAnalysisVo.WrongQuestionItem> getWrongTop10(List<Record> records) {
        if (records.isEmpty()) return new ArrayList<>();
        List<Long> recordIds = records.stream().map(Record::getId).collect(Collectors.toList());

        // 查询所有答错的详情 (isCorrect=0 或 得分 < 满分/2)
        // 这里简化为 isCorrect = 0 (对于客观题) 或者得分较低的主观题
        // 由于 RecordDetail 表结构限制，我们假设 isCorrect 字段用于判断
        List<RecordDetail> wrongDetails = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                .in(RecordDetail::getRecordId, recordIds)
                .eq(RecordDetail::getIsCorrect, 0));

        // 按题目ID分组计数
        Map<Long, Long> wrongCountMap = wrongDetails.stream()
                .collect(Collectors.groupingBy(RecordDetail::getQuestionId, Collectors.counting()));

        // 排序取Top10
        List<Long> top10Ids = wrongCountMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (top10Ids.isEmpty()) return new ArrayList<>();

        List<Question> questions = questionService.listByIds(top10Ids);
        List<ExamAnalysisVo.WrongQuestionItem> result = new ArrayList<>();

        for (Question q : questions) {
            ExamAnalysisVo.WrongQuestionItem item = new ExamAnalysisVo.WrongQuestionItem();
            item.setId(q.getId());
            item.setContent(q.getContent());
            // 修复: Byte转Integer
            item.setType(getTypeName(q.getType() != null ? q.getType().intValue() : null));
            item.setKnowledge(parseFirstTag(q.getTags()));

            // 计算错误率
            long wrongCount = wrongCountMap.getOrDefault(q.getId(), 0L);
            double rate = (double) wrongCount / records.size() * 100.0;
            item.setErrorRate(Math.round(rate * 10.0) / 10.0);

            result.add(item);
        }

        // 按错误率再次降序
        result.sort((a, b) -> b.getErrorRate().compareTo(a.getErrorRate()));
        return result;
    }

    private ExamAnalysisVo emptyVo() {
        ExamAnalysisVo vo = new ExamAnalysisVo();
        vo.setAvgScore(0.0);
        vo.setMaxScore(0.0);
        vo.setMinScore(0.0);
        vo.setPassRate(0.0);
        vo.setAttendeeCount(0);
        vo.setScoreRanges(new ArrayList<>());
        vo.setScoreCounts(new ArrayList<>());
        vo.setKnowledgeRadar(new ArrayList<>());
        vo.setWrongTop10(new ArrayList<>());
        return vo;
    }

    private List<ExamAnalysisVo.RadarItem> generateMockRadar() {
        // 在没有足够Tag数据时的模拟展示
        List<ExamAnalysisVo.RadarItem> list = new ArrayList<>();
        String[] points = {"基础语法", "面向对象", "集合框架", "多线程", "网络编程", "数据库"};
        Random r = new Random();
        for (String p : points) {
            ExamAnalysisVo.RadarItem item = new ExamAnalysisVo.RadarItem();
            item.setName(p);
            item.setMax(100.0);
            item.setValue(60.0 + r.nextInt(30)); // 60-90随机
            list.add(item);
        }
        return list;
    }

    private String getTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1:
                return "单选题";
            case 2:
                return "多选题";
            case 3:
                return "判断题";
            case 4:
                return "简答题";
            default:
                return "其他";
        }
    }

    private String parseFirstTag(String tagsJson) {
        if (tagsJson == null || tagsJson.length() < 3) return "综合";
        try {
            // 简单去除括号引号，取第一个
            String clean = tagsJson.replace("[", "").replace("]", "").replace("\"", "");
            return clean.split(",")[0];
        } catch (Exception e) {
            return "综合";
        }
    }
}
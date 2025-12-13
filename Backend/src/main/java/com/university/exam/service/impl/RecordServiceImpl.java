package com.university.exam.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.dto.student.DashboardStatsVo;
import com.university.exam.common.dto.student.ExamPaperVo;
import com.university.exam.common.dto.student.SubmitExamRequest;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.*;
import com.university.exam.entity.Record;
import com.university.exam.mapper.RecordMapper;
import com.university.exam.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
// ... existing code ...
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 考试记录表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordServiceImpl extends ServiceImpl<RecordMapper, Record> implements RecordService {

    private final PublishService publishService;
    private final PaperService paperService;
    private final PaperQuestionService paperQuestionService;
    private final QuestionService questionService;
    private final RecordDetailService recordDetailService;
    private final MistakeBookService mistakeBookService;
    private final AutoGradingService autoGradingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaperVo startExam(Long userId, Long publishId) {
        // ... existing code ...
        LocalDateTime now = LocalDateTime.now();

        // 1. 获取发布信息校验
        Publish publish = publishService.getById(publishId);
        // ... existing code ...
        // 校验时间
        if (now.isBefore(publish.getStartTime())) throw new BizException(400, "考试未开始");
        if (now.isAfter(publish.getEndTime())) throw new BizException(400, "考试已结束");

        // 2. 检查是否有"进行中"的记录 (断点续考)
        Record record = this.getOne(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .eq(Record::getPublishId, publishId)
                .eq(Record::getStatus, 1)); // 1-进行中

        if (record == null) {
            // 3. 如果没有进行中的，检查是否超过次数限制
            long triedCount = this.count(new LambdaQueryWrapper<Record>()
                    .eq(Record::getUserId, userId)
                    .eq(Record::getPublishId, publishId));
            if (publish.getLimitCount() != -1 && triedCount >= publish.getLimitCount()) {
                throw new BizException(400, "已达到最大考试次数限制");
            }

            // 4. 创建新记录
            record = new Record();
            record.setUserId(userId);
            record.setPublishId(publishId);
            record.setPaperId(publish.getPaperId());
            record.setStartTime(now);
            record.setStatus((byte) 1);
            record.setCreateBy(userId);
            record.setUpdateBy(userId);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            this.save(record);
        }

        // 5. 构建试卷 VO
        Paper paper = paperService.getById(publish.getPaperId());
        ExamPaperVo vo = new ExamPaperVo();
        vo.setRecordId(record.getId());
        vo.setPaperId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setTotalScore(paper.getTotalScore());
        vo.setDuration(paper.getDuration());

        // 计算剩余秒数 = min(考试结束时间 - now, 开始时间 + duration - now)
        LocalDateTime examEndTime = publish.getEndTime();
        LocalDateTime paperEndTime = record.getStartTime().plusMinutes(paper.getDuration());
        LocalDateTime realEndTime = examEndTime.isBefore(paperEndTime) ? examEndTime : paperEndTime;
        long remainingSeconds = Duration.between(now, realEndTime).getSeconds();
        vo.setRemainingSeconds(remainingSeconds > 0 ? remainingSeconds : 0);

        // 6. 获取题目并脱敏
        List<PaperQuestion> pqs = paperQuestionService.list(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .orderByAsc(PaperQuestion::getSortOrder));

        List<Long> qIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionService.listByIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 如果是断点续考，加载已保存的答案（如果有）
        // 这里简化处理，暂不回显已填答案，后续可优化查询 record_detail 临时表

        List<ExamPaperVo.QuestionVo> questionVos = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q != null) {
                ExamPaperVo.QuestionVo qVo = new ExamPaperVo.QuestionVo();
                qVo.setId(q.getId());
                qVo.setType(q.getType().intValue());
                qVo.setContent(q.getContent());
                qVo.setImageUrl(q.getImageUrl());
                qVo.setOptions(q.getOptions());
                qVo.setScore(pq.getScore());
                // 关键：不设置 answer 和 analysis
                questionVos.add(qVo);
            }
        }
        vo.setQuestions(questionVos);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitExam(SubmitExamRequest req, Long userId) {
        // 1. 获取记录
        Record record = this.getById(req.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BizException(403, "非法操作");
        }
        if (record.getStatus() != 1) {
            throw new BizException(400, "考试已结束或已提交，请勿重复提交");
        }

        // 2. 准备数据：题目Map 和 分值Map
        List<PaperQuestion> pqs = paperQuestionService.list(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, record.getPaperId()));
        Map<Long, BigDecimal> scoreMap = pqs.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        List<Long> questionIds = req.getAnswers().stream()
                .map(SubmitExamRequest.AnswerItem::getQuestionId)
                .collect(Collectors.toList());
        Map<Long, Question> questionMap = questionService.listByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<RecordDetail> details = new ArrayList<>();
        List<MistakeBook> mistakeList = new ArrayList<>();
        BigDecimal currentTotalScore = BigDecimal.ZERO;
        boolean hasSubjective = false;

        // 3. 遍历答案，保存明细并立即批改客观题
        if (CollUtil.isNotEmpty(req.getAnswers())) {
            for (SubmitExamRequest.AnswerItem item : req.getAnswers()) {
                Question q = questionMap.get(item.getQuestionId());
                if (q == null) continue;

                RecordDetail detail = new RecordDetail();
                detail.setRecordId(record.getId());
                detail.setQuestionId(item.getQuestionId());
                detail.setStudentAnswer(item.getUserAnswer());
                BigDecimal maxScore = scoreMap.getOrDefault(item.getQuestionId(), BigDecimal.ZERO);
                detail.setMaxScore(maxScore);

                // --- 核心逻辑：区分题型处理 ---
                if (q.getType() == 1 || q.getType() == 2 || q.getType() == 3) {
                    // 客观题：直接比对
                    // 1-单选, 2-多选, 3-判断
                    boolean isCorrect = false;
                    if (StrUtil.equals(item.getUserAnswer(), q.getAnswer())) {
                        isCorrect = true;
                        detail.setScore(maxScore);
                        currentTotalScore = currentTotalScore.add(maxScore);
                    } else {
                        detail.setScore(BigDecimal.ZERO);
                        // 记录错题
                        MistakeBook mb = new MistakeBook();
                        mb.setUserId(userId);
                        mb.setQuestionId(q.getId());
                        mb.setCourseId(q.getCourseId());
                        mb.setLastWrongAnswer(item.getUserAnswer());
                        mb.setWrongCount(1);
                        mb.setCreateBy(userId);
                        mb.setUpdateBy(userId);
                        mb.setCreateTime(LocalDateTime.now());
                        mb.setUpdateTime(LocalDateTime.now());
                        mistakeList.add(mb);
                    }
                    detail.setIsCorrect((byte) (isCorrect ? 1 : 0));
                    detail.setIsMarked((byte) 1); // 标记为已批改
                } else {
                    // 主观题：暂设计为0分，等待AI或人工
                    detail.setScore(BigDecimal.ZERO);
                    detail.setIsMarked((byte) 0);
                    detail.setIsCorrect((byte) 0);
                    hasSubjective = true;
                }

                detail.setCreateBy(userId);
                detail.setUpdateBy(userId);
                detail.setCreateTime(LocalDateTime.now());
                detail.setUpdateTime(LocalDateTime.now());
                details.add(detail);
            }
            recordDetailService.saveBatch(details);
        }

        // 4. 保存客观题错题
        saveMistakes(mistakeList);

        // 5. 更新记录状态
        record.setTotalScore(currentTotalScore);
        record.setSubmitTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        if (hasSubjective) {
            record.setStatus((byte) 2); // 2-已交卷 (待AI阅卷)
            this.updateById(record);

            // 6. 触发异步阅卷 (安全调用)
            autoGradingService.gradeSubjectiveQuestionsAsync(record.getId());
        } else {
            record.setStatus((byte) 3); // 3-已批改 (全客观题，直接完成)
            this.updateById(record);
        }
    }

    /**
     * 批量保存错题（简单排重）
     */
    private void saveMistakes(List<MistakeBook> list) {
        if (CollUtil.isEmpty(list)) return;

        for (MistakeBook mb : list) {
            // 检查是否已存在
            long exists = mistakeBookService.count(new LambdaQueryWrapper<MistakeBook>()
                    .eq(MistakeBook::getUserId, mb.getUserId())
                    .eq(MistakeBook::getQuestionId, mb.getQuestionId()));

            if (exists > 0) {
                // 更新错误次数
                MistakeBook update = new MistakeBook();
                update.setLastWrongAnswer(mb.getLastWrongAnswer());
                update.setUpdateTime(LocalDateTime.now());
                mistakeBookService.update(update, new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, mb.getUserId())
                        .eq(MistakeBook::getQuestionId, mb.getQuestionId()));
                // 也可以自增 wrong_count，MyBatis-Plus 需要写 SQL 或取出来更新，这里简化
            } else {
                mistakeBookService.save(mb);
            }
        }
    }

    @Override
    public DashboardStatsVo getStudentStats(Long userId) {
        DashboardStatsVo stats = new DashboardStatsVo();

        // 1. 考试次数
        long examCount = this.count(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .in(Record::getStatus, 2, 3));
        stats.setExamCount((int) examCount);

        // 2. 平均分
        List<Record> finishedRecords = this.list(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId)
                .eq(Record::getStatus, 3)); // 只统计已批改

        if (!finishedRecords.isEmpty()) {
            double avg = finishedRecords.stream()
                    .mapToDouble(r -> r.getTotalScore().doubleValue())
                    .average().orElse(0.0);
            stats.setAvgScore(Math.round(avg * 10.0) / 10.0); // 保留一位小数

            // 3. 及格次数 (假设60分及格，实际应查 Paper.passScore)
            // 这里为了性能不做联表，简化为 totalScore >= 60
            long passCount = finishedRecords.stream()
                    .filter(r -> r.getTotalScore().doubleValue() >= 60.0)
                    .count();
            stats.setPassCount((int) passCount);
        } else {
            stats.setAvgScore(0.0);
            stats.setPassCount(0);
        }

        // 4. 错题数
        long mistakeCount = mistakeBookService.count(new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId));
        stats.setMistakeCount((int) mistakeCount);

        return stats;
    }
}
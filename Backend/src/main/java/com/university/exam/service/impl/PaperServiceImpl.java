package com.university.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.Paper;
import com.university.exam.entity.PaperQuestion;
import com.university.exam.entity.Question;
import com.university.exam.mapper.PaperMapper;
import com.university.exam.service.PaperQuestionService;
import com.university.exam.service.PaperService;
import com.university.exam.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 试卷服务实现类
 *
 * @author MySQL数据库架构师
 * @version 1.2.0 (增加严格数据校验)
 * @since 2025-12-09
 */
@Service
@RequiredArgsConstructor
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    private final QuestionService questionService;
    private final PaperQuestionService paperQuestionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long randomCreate(RandomPaperRequest req, Long userId) {
        // 1. 初始化
        BigDecimal totalScore = BigDecimal.ZERO;
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        int sortOrder = 1;

        // 校验规则列表非空 (虽然 DTO 已校验，双重保险)
        if (req.getRules() == null || req.getRules().isEmpty()) {
            throw new BizException(400, "组卷规则不能为空");
        }

        // 2. 遍历规则抽题
        for (RuleItem rule : req.getRules()) {
            // 严格校验单条规则
            if (rule.getCount() == null || rule.getCount() <= 0) {
                throw new BizException(400, "规则中的题目数量必须大于0");
            }
            if (rule.getScore() == null || rule.getScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(400, "规则中的题目分值必须大于0");
            }

            // 随机抽取
            List<Question> questions = questionService.getRandomQuestions(req.getCourseId(), rule.getType(), rule.getCount());
            
            // 校验题库充足性
            if (questions.size() < rule.getCount()) {
                throw new BizException(400, String.format("题库资源不足：类型[%s]需要%d题，当前题库仅有%d题，请先扩充题库", 
                        getTypeName(rule.getType()), rule.getCount(), questions.size()));
            }

            // 构建关联关系
            for (Question q : questions) {
                PaperQuestion pq = new PaperQuestion();
                pq.setQuestionId(q.getId());
                pq.setScore(rule.getScore());
                pq.setSortOrder(sortOrder++);
                paperQuestions.add(pq);
                
                // 累加总分
                totalScore = totalScore.add(rule.getScore());
            }
        }

        // 3. 核心分数校验
        validatePaperScore(totalScore, req.getPassScore());

        // 4. 保存试卷
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setDuration(req.getDuration());
        paper.setTotalScore(totalScore);
        // 如果未传及格分，默认设为总分的 60%
        paper.setPassScore(req.getPassScore() != null ? req.getPassScore() : totalScore.multiply(new BigDecimal("0.6")));
        paper.setStatus((byte) 1);
        paper.setDifficulty((byte) 2); // 默认为中等，后续可优化为计算平均难度
        paper.setCreateBy(userId);
        paper.setUpdateBy(userId);
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        
        this.save(paper);

        // 5. 保存关联关系
        savePaperQuestions(paper.getId(), paperQuestions, userId);

        return paper.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long manualCreate(ManualPaperRequest req, Long userId) {
        BigDecimal totalScore = BigDecimal.ZERO;
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        int sortOrder = 1;

        if (req.getQuestionList() == null || req.getQuestionList().isEmpty()) {
            throw new BizException(400, "题目列表不能为空");
        }

        // 2. 遍历题目列表
        for (QuestionItem item : req.getQuestionList()) {
            if (item.getScore() == null || item.getScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(400, "题目分值必须大于0");
            }

            PaperQuestion pq = new PaperQuestion();
            pq.setQuestionId(item.getQuestionId());
            pq.setScore(item.getScore());
            pq.setSortOrder(sortOrder++);
            paperQuestions.add(pq);

            totalScore = totalScore.add(item.getScore());
        }

        // 3. 核心分数校验
        validatePaperScore(totalScore, req.getPassScore());

        // 4. 保存试卷
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setDuration(req.getDuration());
        paper.setTotalScore(totalScore);
        paper.setPassScore(req.getPassScore() != null ? req.getPassScore() : totalScore.multiply(new BigDecimal("0.6")));
        paper.setStatus((byte) 1);
        paper.setDifficulty((byte) 2);
        paper.setCreateBy(userId);
        paper.setUpdateBy(userId);
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        
        this.save(paper);

        // 5. 保存关联
        savePaperQuestions(paper.getId(), paperQuestions, userId);

        return paper.getId();
    }

    /**
     * 校验试卷分数逻辑
     */
    private void validatePaperScore(BigDecimal totalScore, BigDecimal passScore) {
        // 1. 总分必须大于0
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "试卷总分必须大于0，请添加题目");
        }

        // 2. 校验及格分
        if (passScore != null) {
            if (passScore.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "及格分数不能为负数");
            }
            // 核心校验：及格分不能大于总分
            if (passScore.compareTo(totalScore) > 0) {
                throw new BizException(400, String.format("及格分数(%.1f)不能高于试卷总分(%.1f)，请调整规则或降低及格分", 
                        passScore.doubleValue(), totalScore.doubleValue()));
            }
        }
    }

    private void savePaperQuestions(Long paperId, List<PaperQuestion> list, Long userId) {
        for (PaperQuestion pq : list) {
            pq.setPaperId(paperId);
            pq.setCreateBy(userId);
            pq.setUpdateBy(userId);
            pq.setCreateTime(LocalDateTime.now());
            pq.setUpdateTime(LocalDateTime.now());
        }
        paperQuestionService.saveBatch(list);
    }

    private String getTypeName(Integer type) {
        switch (type) {
            case 1: return "单选";
            case 2: return "多选";
            case 3: return "判断";
            case 4: return "简答";
            case 5: return "填空";
            default: return "未知";
        }
    }

    @Override
    public PaperDetailVo getPaperDetail(Long paperId) {
        Paper paper = this.getById(paperId);
        if (paper == null) {
            throw new BizException(404, "试卷不存在");
        }

        PaperDetailVo vo = new PaperDetailVo();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setCourseId(paper.getCourseId());
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setDuration(paper.getDuration());
        vo.setDifficulty(paper.getDifficulty());

        // 获取题目列表
        List<PaperQuestion> pqs = paperQuestionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .orderByAsc(PaperQuestion::getSortOrder)
        );

        List<Long> questionIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, Question> questionMap = questionIds.isEmpty() ? java.util.Collections.emptyMap() :
            questionService.listByIds(questionIds).stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));

        java.util.Map<Long, PaperQuestion> pqMap = pqs.stream()
            .collect(java.util.stream.Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq));

        List<QuestionDetailItem> questions = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMap.get(pq.getQuestionId());
            if (q == null) continue;

            QuestionDetailItem item = new QuestionDetailItem();
            item.setId(q.getId());
            item.setType(q.getType().intValue());
            item.setContent(q.getContent());
            item.setOptions(q.getOptions());
            item.setAnswer(q.getAnswer());
            item.setAnalysis(q.getAnalysis());
            item.setDifficulty(q.getDifficulty().intValue());
            item.setScore(pq.getScore());
            item.setSortOrder(pq.getSortOrder());
            questions.add(item);
        }
        vo.setQuestions(questions);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuestionToPaper(Long paperId, Long questionId, BigDecimal score, Long userId) {
        Paper paper = this.getById(paperId);
        if (paper == null) {
            throw new BizException(404, "试卷不存在");
        }

        // 检查题目是否已存在
        long exists = paperQuestionService.count(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getQuestionId, questionId)
        );
        if (exists > 0) {
            throw new BizException(400, "该题目已在试卷中");
        }

        // 获取最大排序号
        Integer maxSort = paperQuestionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .orderByDesc(PaperQuestion::getSortOrder)
                .last("LIMIT 1")
        ).stream().findFirst().map(PaperQuestion::getSortOrder).orElse(0);

        // 添加题目
        PaperQuestion pq = new PaperQuestion();
        pq.setPaperId(paperId);
        pq.setQuestionId(questionId);
        pq.setScore(score);
        pq.setSortOrder(maxSort + 1);
        pq.setCreateBy(userId);
        pq.setUpdateBy(userId);
        pq.setCreateTime(LocalDateTime.now());
        pq.setUpdateTime(LocalDateTime.now());
        paperQuestionService.save(pq);

        // 更新试卷总分
        paper.setTotalScore(paper.getTotalScore().add(score));
        paper.setUpdateBy(userId);
        paper.setUpdateTime(LocalDateTime.now());
        this.updateById(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeQuestionFromPaper(Long paperId, Long questionId, Long userId) {
        Paper paper = this.getById(paperId);
        if (paper == null) {
            throw new BizException(404, "试卷不存在");
        }

        // 查找关联记录
        PaperQuestion pq = paperQuestionService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getQuestionId, questionId)
        );
        if (pq == null) {
            throw new BizException(404, "题目不在试卷中");
        }

        // 删除关联
        paperQuestionService.removeById(pq.getId());

        // 更新试卷总分
        paper.setTotalScore(paper.getTotalScore().subtract(pq.getScore()));
        paper.setUpdateBy(userId);
        paper.setUpdateTime(LocalDateTime.now());
        this.updateById(paper);
    }
}
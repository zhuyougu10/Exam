package com.university.exam.service.impl;

import com.university.exam.common.exception.BizException;
import com.university.exam.entity.Paper;
import com.university.exam.entity.PaperQuestion;
import com.university.exam.entity.Question;
import com.university.exam.mapper.PaperMapper;
import com.university.exam.service.PaperQuestionService;
import com.university.exam.service.PaperService;
import com.university.exam.service.QuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 试卷模板表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
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

        // 2. 遍历规则抽题
        for (RuleItem rule : req.getRules()) {
            if (rule.getCount() <= 0) continue;
            // 校验单题分数合理性
            if (rule.getScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(400, "单题分值必须大于0");
            }

            // 随机抽取
            List<Question> questions = questionService.getRandomQuestions(req.getCourseId(), rule.getType(), rule.getCount());
            
            if (questions.size() < rule.getCount()) {
                throw new BizException(400, String.format("题库资源不足：类型[%s]需要%d题，仅有%d题", 
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

        // 3. 业务逻辑核心校验
        validatePaperScore(totalScore, req.getPassScore());

        // 4. 保存试卷
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setDuration(req.getDuration());
        paper.setTotalScore(totalScore);
        // 如果前端没传及格分，默认总分60%
        paper.setPassScore(req.getPassScore() != null ? req.getPassScore() : totalScore.multiply(new BigDecimal("0.6")));
        paper.setStatus((byte) 1);
        paper.setDifficulty((byte) 2);
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
        // 1. 初始化
        BigDecimal totalScore = BigDecimal.ZERO;
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        int sortOrder = 1;

        // 2. 遍历题目列表构建关联
        for (QuestionItem item : req.getQuestionList()) {
            // 校验分数
            if (item.getScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(400, "题目分值必须大于0");
            }

            PaperQuestion pq = new PaperQuestion();
            pq.setQuestionId(item.getQuestionId());
            pq.setScore(item.getScore());
            pq.setSortOrder(sortOrder++);
            paperQuestions.add(pq);

            totalScore = totalScore.add(item.getScore());
        }

        // 3. 业务逻辑核心校验
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

        // 2. 如果指定了及格分，及格分必须 <= 总分
        if (passScore != null) {
            if (passScore.compareTo(totalScore) > 0) {
                throw new BizException(400, String.format("及格分数(%.1f)不能高于试卷总分(%.1f)", 
                        passScore.doubleValue(), totalScore.doubleValue()));
            }
            if (passScore.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "及格分数不能为负数");
            }
        }
    }

    /**
     * 批量保存题目关联
     */
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
}
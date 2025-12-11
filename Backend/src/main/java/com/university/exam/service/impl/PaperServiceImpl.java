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
        // 1. 预检查与总分计算
        BigDecimal totalScore = BigDecimal.ZERO;
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        int sortOrder = 1;

        // 临时创建 Paper 对象先保存，获取 ID
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setDuration(req.getDuration() != null ? req.getDuration() : 120);
        paper.setStatus((byte) 1); // 默认启用
        paper.setDifficulty((byte) 2); // 默认中等，后续可优化根据题目计算
        paper.setCreateBy(userId);
        paper.setUpdateBy(userId);
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        
        // 2. 遍历规则抽题
        for (RuleItem rule : req.getRules()) {
            if (rule.getCount() <= 0) continue;

            // 随机抽取
            List<Question> questions = questionService.getRandomQuestions(req.getCourseId(), rule.getType(), rule.getCount());
            
            if (questions.size() < rule.getCount()) {
                throw new BizException(400, String.format("题库不足：类型[%s]需要%d题，仅有%d题", 
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

        // 3. 完善并保存试卷
        paper.setTotalScore(totalScore);
        // 及格分默认 60%
        paper.setPassScore(totalScore.multiply(new BigDecimal("0.6")));
        this.save(paper);

        // 4. 保存关联关系
        for (PaperQuestion pq : paperQuestions) {
            pq.setPaperId(paper.getId());
            pq.setCreateBy(userId);
            pq.setUpdateBy(userId);
            pq.setCreateTime(LocalDateTime.now());
            pq.setUpdateTime(LocalDateTime.now());
        }
        paperQuestionService.saveBatch(paperQuestions);

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
            PaperQuestion pq = new PaperQuestion();
            pq.setQuestionId(item.getQuestionId());
            pq.setScore(item.getScore());
            pq.setSortOrder(sortOrder++);
            paperQuestions.add(pq);

            totalScore = totalScore.add(item.getScore());
        }

        // 3. 保存试卷
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setDuration(req.getDuration() != null ? req.getDuration() : 120);
        paper.setTotalScore(totalScore);
        paper.setPassScore(totalScore.multiply(new BigDecimal("0.6")));
        paper.setStatus((byte) 1);
        paper.setDifficulty((byte) 2);
        paper.setCreateBy(userId);
        paper.setUpdateBy(userId);
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        
        this.save(paper);

        // 4. 保存关联
        for (PaperQuestion pq : paperQuestions) {
            pq.setPaperId(paper.getId());
            pq.setCreateBy(userId);
            pq.setUpdateBy(userId);
            pq.setCreateTime(LocalDateTime.now());
            pq.setUpdateTime(LocalDateTime.now());
        }
        paperQuestionService.saveBatch(paperQuestions);

        return paper.getId();
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
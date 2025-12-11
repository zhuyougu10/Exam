package com.university.exam.service;

import com.university.exam.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 试卷模板表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface PaperService extends IService<Paper> {

    /**
     * 智能/随机组卷
     *
     * @param request 组卷请求参数
     * @param userId  创建人ID
     * @return 试卷ID
     */
    Long randomCreate(RandomPaperRequest request, Long userId);

    /**
     * 手动组卷
     *
     * @param request 组卷请求参数
     * @param userId  创建人ID
     * @return 试卷ID
     */
    Long manualCreate(ManualPaperRequest request, Long userId);

    // ==================== DTO 定义 ====================

    @Data
    class RandomPaperRequest {
        private Long courseId;
        private String title;
        private Integer duration; // 考试时长(分钟)
        private List<RuleItem> rules;
    }

    @Data
    class RuleItem {
        private Integer type;  // 题目类型
        private Integer count; // 题目数量
        private BigDecimal score; // 单题分值
    }

    @Data
    class ManualPaperRequest {
        private Long courseId;
        private String title;
        private Integer duration; // 考试时长(分钟)
        private List<QuestionItem> questionList;
    }

    @Data
    class QuestionItem {
        private Long questionId;
        private BigDecimal score;
    }
}
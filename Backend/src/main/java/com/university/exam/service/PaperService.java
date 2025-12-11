package com.university.exam.service;

import com.university.exam.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
     */
    Long randomCreate(RandomPaperRequest request, Long userId);

    /**
     * 手动组卷
     */
    Long manualCreate(ManualPaperRequest request, Long userId);

    // ==================== DTO 定义 (增加校验注解) ====================

    @Data
    class RandomPaperRequest {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;

        @NotBlank(message = "试卷标题不能为空")
        private String title;

        @NotNull(message = "考试时长不能为空")
        @Min(value = 1, message = "考试时长必须大于0分钟")
        @Max(value = 480, message = "考试时长不能超过480分钟")
        private Integer duration;

        @Min(value = 0, message = "及格分不能为负数")
        private BigDecimal passScore;

        @NotEmpty(message = "组卷规则不能为空")
        @Valid // 级联校验列表中的对象
        private List<RuleItem> rules;
    }

    @Data
    class RuleItem {
        @NotNull(message = "题目类型不能为空")
        private Integer type;

        @Min(value = 0, message = "题目数量不能为负数")
        private Integer count;

        @DecimalMin(value = "0.0", message = "单题分值不能为负数")
        private BigDecimal score;
    }

    @Data
    class ManualPaperRequest {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;

        @NotBlank(message = "试卷标题不能为空")
        private String title;

        @NotNull(message = "考试时长不能为空")
        @Min(value = 1, message = "考试时长必须大于0分钟")
        @Max(value = 480, message = "考试时长不能超过480分钟")
        private Integer duration;

        @Min(value = 0, message = "及格分不能为负数")
        private BigDecimal passScore;

        @NotEmpty(message = "题目列表不能为空")
        @Valid
        private List<QuestionItem> questionList;
    }

    @Data
    class QuestionItem {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        @DecimalMin(value = "0.1", message = "单题分值必须大于0")
        private BigDecimal score;
    }
}
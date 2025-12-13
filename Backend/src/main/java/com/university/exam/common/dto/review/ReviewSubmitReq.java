package com.university.exam.common.dto.review;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师人工复核/改卷请求参数
 *
 * @author MySQL数据库架构师
 * @since 2025-12-14
 */
@Data
public class ReviewSubmitReq {
    
    @NotNull(message = "记录ID不能为空")
    private Long recordId;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "分数不能为空")
    @DecimalMin(value = "0.0", message = "分数不能为负数")
    private BigDecimal score;

    /**
     * 教师评语 (可选)
     */
    private String comment;
}
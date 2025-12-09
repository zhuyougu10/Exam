package com.university.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 答题明细表
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Getter
@Setter
@TableName("exam_record_detail")
public class RecordDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 考试记录ID
     */
    @TableField("record_id")
    private Long recordId;

    /**
     * 题目ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 学生答案
     */
    @TableField("student_answer")
    private String studentAnswer;

    /**
     * 得分
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 满分
     */
    @TableField("max_score")
    private BigDecimal maxScore;

    /**
     * AI评语
     */
    @TableField("ai_comment")
    private String aiComment;

    /**
     * 是否正确（1-正确, 0-错误）
     */
    @TableField("is_correct")
    private Byte isCorrect;

    /**
     * 是否已批改（1-已批改, 0-未批改）
     */
    @TableField("is_marked")
    private Byte isMarked;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 删除状态（0-正常, 1-已删除）
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}

package com.university.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 题库表
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Getter
@Setter
@TableName("exam_question")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属课程ID
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 题目类型（1-单选, 2-多选, 3-判断, 4-简答）
     */
    @TableField("type")
    private Byte type;

    /**
     * 题目内容
     */
    @TableField("content")
    private String content;

    /**
     * 题目图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 选项（JSON格式，如["A选项", "B选项"]）
     */
    @TableField("options")
    private String options;

    /**
     * 答案
     */
    @TableField("answer")
    private String answer;

    /**
     * 解析
     */
    @TableField("analysis")
    private String analysis;

    /**
     * 难度（1-简单, 2-中等, 3-困难）
     */
    @TableField("difficulty")
    private Byte difficulty;

    /**
     * 标签（JSON格式，如["章节1", "重点"]）
     */
    @TableField("tags")
    private String tags;

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

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
 * 考试发布/场次表
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Getter
@Setter
@TableName("exam_publish")
public class Publish implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 试卷模板ID
     */
    @TableField("paper_id")
    private Long paperId;

    /**
     * 考试标题
     */
    @TableField("title")
    private String title;

    /**
     * 目标部门/班级ID列表（JSON格式）
     */
    @TableField("target_dept_ids")
    private String targetDeptIds;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 限制次数
     */
    @TableField("limit_count")
    private Integer limitCount;

    /**
     * 考试密码
     */
    @TableField("password")
    private String password;

    /**
     * 状态（1-启用, 0-禁用）
     */
    @TableField("status")
    private Byte status;

    /**
     * 是否允许考前看解析：0-否，1-是
     * 新增字段
     */
    @TableField("allow_early_analysis")
    private Byte allowEarlyAnalysis;

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
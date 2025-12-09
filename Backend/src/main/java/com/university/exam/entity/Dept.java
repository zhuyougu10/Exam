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
 * 部门/班级表
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Getter
@Setter
@TableName("sys_dept")
public class Dept implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 部门/班级名称
     */
    @TableField("dept_name")
    private String deptName;

    /**
     * 父部门ID（0-根部门）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 祖先部门ID列表，逗号分隔
     */
    @TableField("ancestors")
    private String ancestors;

    /**
     * 排序字段
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 负责人姓名
     */
    @TableField("leader")
    private String leader;

    /**
     * 类别（1-学院, 2-系, 3-班级）
     */
    @TableField("category")
    private Byte category;

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

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
 * 监考日志表
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Getter
@Setter
@TableName("exam_proctor_log")
public class ProctorLog implements Serializable {

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
     * 操作类型（switch_screen, leave_page, env_abnormal）
     */
    @TableField("action_type")
    private String actionType; // 修正为 String

    /**
     * 日志内容
     */
    @TableField("content")
    private String content;

    /**
     * 截图URL
     */
    @TableField("img_snapshot")
    private String imgSnapshot;

    /**
     * 发生时间
     */
    @TableField("happen_time")
    private LocalDateTime happenTime;

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
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
 * 考试记录表
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Getter
@Setter
@TableName("exam_record")
public class Record implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 考试发布ID
     */
    @TableField("publish_id")
    private Long publishId;

    /**
     * 试卷ID
     */
    @TableField("paper_id")
    private Long paperId;

    /**
     * 总得分
     */
    @TableField("total_score")
    private BigDecimal totalScore;

    /**
     * 用户IP
     */
    @TableField("user_ip")
    private String userIp;

    /**
     * 用户设备信息
     */
    @TableField("user_device")
    private String userDevice;

    /**
     * 开始考试时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 提交时间
     */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /**
     * 状态（0-未开始, 1-进行中, 2-已交卷, 3-已批卷）
     */
    @TableField("status")
    private Byte status;

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

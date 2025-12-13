package com.university.exam.common.dto.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 监考日志上报 DTO
 */
@Data
public class ProctorLogDto {
    
    @NotNull(message = "考试记录ID不能为空")
    private Long recordId;

    /**
     * 动作类型: 
     * switch_screen (切屏)
     * leave_page (离开页面)
     * env_abnormal (环境异常/人脸丢失)
     * snapshot (定时抓拍)
     */
    @NotBlank(message = "动作类型不能为空")
    private String actionType;

    /**
     * 抓拍快照 URL (可选)
     */
    private String imgSnapshot;

    /**
     * 详细内容/备注 (可选)
     */
    private String content;
}
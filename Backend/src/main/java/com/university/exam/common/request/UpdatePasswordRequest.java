package com.university.exam.common.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求参数
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-10
 */
@Data
public class UpdatePasswordRequest {
    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    
    /**
     * 确认新密码 (前端校验即可，后端可选)
     */
    private String confirmPassword;
}
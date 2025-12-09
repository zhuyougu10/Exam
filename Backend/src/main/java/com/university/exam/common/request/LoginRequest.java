package com.university.exam.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数类
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Data
public class LoginRequest {
    /**
     * 用户名：非空字符串，长度限制在4-32字符范围内
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度必须在4-32个字符之间")
    private String username;

    /**
     * 密码：非空字符串，长度限制在6-64字符范围内
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在6-64个字符之间")
    private String password;
}
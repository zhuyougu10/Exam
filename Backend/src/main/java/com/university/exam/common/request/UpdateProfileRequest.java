package com.university.exam.common.request;

import lombok.Data;

/**
 * 更新个人信息请求参数
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-10
 */
@Data
public class UpdateProfileRequest {
    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 性别 (0-女, 1-男, 2-未知) - 预留字段
     */
    private Integer gender;
}
package com.university.exam.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义业务异常类
 * 用于处理业务逻辑中的异常情况，包含错误码和错误消息
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码：200-成功，500-服务器错误，401-未授权，403-权限不足及其他业务错误码
     */
    private int code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 构造方法
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法，默认错误码为500
     *
     * @param message 错误消息
     */
    public BizException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造方法，包含异常链
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}
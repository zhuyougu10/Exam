package com.university.exam.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果类
 *
 * @param <T> 响应数据类型
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态码：200-成功，500-服务器错误，401-未授权，403-权限不足
     */
    private int code;

    /**
     * 操作结果描述或错误信息
     */
    private String msg;

    /**
     * 业务数据，成功时返回具体数据，失败时可为null
     */
    private T data;

    /**
     * 构造方法私有化，禁止外部直接实例化
     */
    private Result() {
    }

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 创建成功响应
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 创建带自定义消息的成功响应
     *
     * @param data 业务数据
     * @param msg  自定义消息
     * @param <T>  数据类型
     * @return 成功响应结果
     */
    public static <T> Result<T> success(T data, String msg) {
        return new Result<>(200, msg, data);
    }

    /**
     * 创建错误响应
     *
     * @param code 错误码
     * @param msg  错误消息
     * @param <T>  数据类型
     * @return 错误响应结果
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 创建带数据的错误响应
     *
     * @param code 错误码
     * @param msg  错误消息
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 错误响应结果
     */
    public static <T> Result<T> error(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}
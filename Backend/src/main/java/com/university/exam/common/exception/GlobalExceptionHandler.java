package com.university.exam.common.exception;

import com.university.exam.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 * 统一处理系统中的所有异常，转换为标准的Result<T>响应
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     *
     * @param e 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        // 记录业务异常日志，便于调试
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage(), e);
        // 返回业务异常的错误码和消息
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理认证异常（未授权）
     *
     * @param e 认证异常
     * @return 统一响应结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage(), e);
        // 处理不同类型的认证异常
        String message;
        if (e instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        } else {
            message = "未授权访问";
        }
        return Result.error(401, message);
    }

    /**
     * 处理权限不足异常
     *
     * @param e 权限不足异常
     * @return 统一响应结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足异常: {}", e.getMessage(), e);
        return Result.error(403, "权限不足，无法访问该资源");
    }

    /**
     * 处理404异常
     *
     * @param e 404异常
     * @return 统一响应结果
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("404异常: {}", e.getMessage(), e);
        return Result.error(404, "请求的资源不存在");
    }

    /**
     * 处理通用异常
     *
     * @param e 通用异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        // 记录详细异常日志，便于调试
        log.error("系统异常: {}", e.getMessage(), e);
        
        // 生产环境隐藏详细异常信息，只返回标准错误提示
        // 可以通过环境变量或配置开关控制是否返回详细信息
        String message = "服务器内部错误，请稍后重试";
        
        // 开发环境可以返回详细异常信息
        // if (isDevEnvironment()) {
        //     message = e.getMessage();
        // }
        
        return Result.error(500, message);
    }
}
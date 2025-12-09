package com.university.exam.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 操作日志拦截器
 * 记录请求的开始时间、URL、客户端IP地址、User-Agent信息以及请求处理耗时
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {
    /**
     * 请求开始时间的键名
     */
    private static final String START_TIME_KEY = "startTime";

    /**
     * 请求处理前的回调方法
     * 记录请求开始时间、请求URL、客户端IP地址和User-Agent信息
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器对象
     * @return 是否继续执行后续拦截器和处理器
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_KEY, startTime);

        // 2. 获取请求信息
        String url = request.getRequestURL().toString();
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 3. 记录请求日志
        log.info("请求开始 - URL: {}, IP: {}, User-Agent: {}", url, ip, userAgent);

        return true;
    }

    /**
     * 请求处理完成后的回调方法
     * 计算并记录请求处理耗时
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器对象
     * @param ex 异常对象
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 1. 获取请求开始时间
        Long startTime = (Long) request.getAttribute(START_TIME_KEY);
        if (startTime == null) {
            return;
        }

        // 2. 计算请求处理耗时
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        // 3. 获取请求信息
        String url = request.getRequestURL().toString();
        int status = response.getStatus();

        // 4. 记录请求结束日志
        log.info("请求结束 - URL: {}, 状态码: {}, 耗时: {}ms", url, status, elapsedTime);
    }

    /**
     * 获取客户端真实IP地址
     * 处理反向代理和负载均衡的情况
     *
     * @param request 请求对象
     * @return 客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是IPv4地址，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
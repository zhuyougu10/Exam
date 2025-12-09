package com.university.exam.config;

import com.university.exam.common.interceptor.LogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 配置拦截器、资源处理、视图解析等Web相关配置
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final LogInterceptor logInterceptor;

    /**
     * 构造方法
     *
     * @param logInterceptor 操作日志拦截器
     */
    public WebMvcConfig(LogInterceptor logInterceptor) {
        this.logInterceptor = logInterceptor;
    }

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册操作日志拦截器，设置拦截路径为/**（所有请求）
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");
    }
}
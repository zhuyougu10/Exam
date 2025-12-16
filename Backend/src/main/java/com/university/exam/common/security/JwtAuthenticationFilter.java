package com.university.exam.common.security;

import com.university.exam.common.utils.JwtUtils;
import com.university.exam.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JWT认证过滤器
 * 用于验证JWT Token并设置认证信息到SecurityContextHolder
 * 整合Redis进行Token验证和管理
 *
 * @author MySQL数据库架构师
 * @version 1.1.0
 * @since 2025-12-09
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;

    /**
     * 构造方法
     *
     * @param jwtUtils JWT工具类
     * @param tokenService Token服务（Redis管理）
     */
    public JwtAuthenticationFilter(JwtUtils jwtUtils, TokenService tokenService) {
        this.jwtUtils = jwtUtils;
        this.tokenService = tokenService;
    }

    /**
     * 过滤请求，验证JWT Token
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从HTTP请求头中提取Authorization字段
            String authHeader = request.getHeader("Authorization");
            String token = null;

            // 2. 检查Authorization格式是否为"Bearer xxx"
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // 3. 验证Token（同时验证JWT和Redis）
            if (token != null && tokenService.validateToken(token)) {
                // 4. 优先从Redis获取用户信息
                Map<String, Object> userInfo = tokenService.getUserInfo(token);
                
                Long userId;
                Integer role;
                
                if (userInfo != null) {
                    // 从Redis获取用户信息
                    userId = ((Number) userInfo.get("userId")).longValue();
                    role = ((Number) userInfo.get("role")).intValue();
                    
                    // 刷新Token过期时间（滑动过期）
                    tokenService.refreshToken(token);
                } else {
                    // 降级：从JWT中获取用户信息
                    Claims claims = jwtUtils.parseToken(token);
                    userId = claims.get("userId", Long.class);
                    role = claims.get("role", Integer.class);
                }

                // 5. 构建权限列表
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                // 6. 构建UsernamePasswordAuthenticationToken对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. 将认证信息存入SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException e) {
            // 8. 处理Token无效、过期或缺失等情况
            log.warn("JWT认证失败: {}", e.getMessage());
            // 清除SecurityContext
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("JWT认证过滤器异常: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // 9. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
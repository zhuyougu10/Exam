package com.university.exam.common.security;

import com.university.exam.common.utils.JwtUtils;
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

/**
 * JWT认证过滤器
 * 用于验证JWT Token并设置认证信息到SecurityContextHolder
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    /**
     * 构造方法
     *
     * @param jwtUtils JWT工具类
     */
    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
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

            // 3. 验证并解析JWT Token
            if (token != null && jwtUtils.validateToken(token)) {
                Claims claims = jwtUtils.parseToken(token);

                // 4. 提取用户信息
                Long userId = claims.get("userId", Long.class);
                Integer role = claims.get("role", Integer.class);

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
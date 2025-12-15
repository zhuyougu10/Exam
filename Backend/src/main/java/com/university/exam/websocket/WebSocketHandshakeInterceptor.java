package com.university.exam.websocket;

import com.university.exam.common.utils.JwtUtils;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器
 * 用于在WebSocket连接建立前进行JWT认证
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    public WebSocketHandshakeInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = null;
            
            // 从URL参数中获取token
            if (request instanceof ServletServerHttpRequest servletRequest) {
                token = servletRequest.getServletRequest().getParameter("token");
            }
            
            // 如果URL参数中没有token，尝试从Header获取
            if (!StringUtils.hasText(token)) {
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket连接失败：缺少token");
                return false;
            }

            // 验证token
            if (!jwtUtils.validateToken(token)) {
                log.warn("WebSocket连接失败：token无效");
                return false;
            }

            // 解析用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            Integer role = jwtUtils.getRoleFromToken(token);
            
            // 将用户信息存入attributes，供WebSocket处理器使用
            attributes.put("userId", userId);
            attributes.put("role", role);
            
            log.info("WebSocket握手成功，用户ID: {}", userId);
            return true;
            
        } catch (JwtException e) {
            log.error("WebSocket握手失败：JWT解析错误", e);
            return false;
        } catch (Exception e) {
            log.error("WebSocket握手失败：未知错误", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理，可以记录日志等
        if (exception != null) {
            log.error("WebSocket握手后发生异常", exception);
        }
    }
}

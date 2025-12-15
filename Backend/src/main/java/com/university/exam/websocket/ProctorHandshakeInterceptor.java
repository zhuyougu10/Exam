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
 * 监考WebSocket握手拦截器
 * 用于在监考WebSocket连接建立前进行JWT认证和参数提取
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Slf4j
@Component
public class ProctorHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    public ProctorHandshakeInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = null;
            String publishIdStr = null;

            // 从URL参数中获取token和publishId
            if (request instanceof ServletServerHttpRequest servletRequest) {
                token = servletRequest.getServletRequest().getParameter("token");
                publishIdStr = servletRequest.getServletRequest().getParameter("publishId");
            }

            // 如果URL参数中没有token，尝试从Header获取
            if (!StringUtils.hasText(token)) {
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (!StringUtils.hasText(token)) {
                log.warn("监考WebSocket连接失败：缺少token");
                return false;
            }

            if (!StringUtils.hasText(publishIdStr)) {
                log.warn("监考WebSocket连接失败：缺少publishId");
                return false;
            }

            // 验证token
            if (!jwtUtils.validateToken(token)) {
                log.warn("监考WebSocket连接失败：token无效");
                return false;
            }

            // 解析用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            Integer role = jwtUtils.getRoleFromToken(token);

            // 验证是否为教师或管理员 (role: 1-学生, 2-教师, 3-管理员)
            if (role == null || role < 2) {
                log.warn("监考WebSocket连接失败：用户 {} 无监考权限，角色: {}", userId, role);
                return false;
            }

            Long publishId = Long.parseLong(publishIdStr);

            // 将用户信息存入attributes
            attributes.put("userId", userId);
            attributes.put("role", role);
            attributes.put("publishId", publishId);

            log.info("监考WebSocket握手成功，教师ID: {}, 考试ID: {}", userId, publishId);
            return true;

        } catch (NumberFormatException e) {
            log.error("监考WebSocket握手失败：publishId格式错误", e);
            return false;
        } catch (JwtException e) {
            log.error("监考WebSocket握手失败：JWT解析错误", e);
            return false;
        } catch (Exception e) {
            log.error("监考WebSocket握手失败：未知错误", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("监考WebSocket握手后发生异常", exception);
        }
    }
}

package com.university.exam.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知WebSocket处理器
 * 管理WebSocket连接的生命周期和消息处理
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Slf4j
@Component
public class NoticeWebSocketHandler extends TextWebSocketHandler {

    /**
     * 存储所有在线用户的WebSocket会话
     * Key: 用户ID, Value: WebSocket会话
     */
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public NoticeWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * WebSocket连接建立后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            // 如果用户已有连接，关闭旧连接
            WebSocketSession oldSession = USER_SESSIONS.get(userId);
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close(CloseStatus.NORMAL.withReason("新连接建立，关闭旧连接"));
                } catch (IOException e) {
                    log.warn("关闭旧WebSocket连接失败", e);
                }
            }
            
            USER_SESSIONS.put(userId, session);
            log.info("WebSocket连接建立，用户ID: {}，当前在线用户数: {}", userId, USER_SESSIONS.size());
            
            // 发送连接成功消息
            sendMessage(userId, WebSocketMessage.success("connected", "WebSocket连接成功"));
        }
    }

    /**
     * 接收到客户端消息时调用
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserId(session);
        String payload = message.getPayload();
        log.debug("收到用户 {} 的消息: {}", userId, payload);

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            // 处理心跳消息
            if ("ping".equals(wsMessage.getType())) {
                sendMessage(userId, WebSocketMessage.success("pong", "心跳响应"));
                return;
            }
            
            // 其他消息类型可以在这里扩展处理
            log.info("用户 {} 发送消息类型: {}", userId, wsMessage.getType());
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    /**
     * WebSocket传输错误时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserId(session);
        log.error("WebSocket传输错误，用户ID: {}", userId, exception);
        removeSession(userId, session);
    }

    /**
     * WebSocket连接关闭后调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserId(session);
        removeSession(userId, session);
        log.info("WebSocket连接关闭，用户ID: {}，状态: {}，当前在线用户数: {}", 
                userId, status, USER_SESSIONS.size());
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId 用户ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessage(Long userId, WebSocketMessage message) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session == null || !session.isOpen()) {
            log.debug("用户 {} 不在线，无法发送消息", userId);
            return false;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("向用户 {} 发送消息成功: {}", userId, message.getType());
            return true;
        } catch (IOException e) {
            log.error("向用户 {} 发送消息失败", userId, e);
            return false;
        }
    }

    /**
     * 向多个用户发送消息
     *
     * @param userIds 用户ID列表
     * @param message 消息内容
     */
    public void sendMessageToUsers(Iterable<Long> userIds, WebSocketMessage message) {
        for (Long userId : userIds) {
            sendMessage(userId, message);
        }
    }

    /**
     * 向所有在线用户广播消息
     *
     * @param message 消息内容
     */
    public void broadcastMessage(WebSocketMessage message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            log.error("序列化广播消息失败", e);
            return;
        }

        USER_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    log.error("向用户 {} 广播消息失败", userId, e);
                }
            }
        });
        log.info("广播消息完成，消息类型: {}，在线用户数: {}", message.getType(), USER_SESSIONS.size());
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return USER_SESSIONS.size();
    }

    /**
     * 从session中获取用户ID
     */
    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? (Long) userId : null;
    }

    /**
     * 移除会话
     */
    private void removeSession(Long userId, WebSocketSession session) {
        if (userId != null) {
            USER_SESSIONS.remove(userId, session);
        }
    }
}

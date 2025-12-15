package com.university.exam.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监考WebSocket处理器
 * 用于教师实时监控学生考试状态
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Slf4j
@Component
public class ProctorWebSocketHandler extends TextWebSocketHandler {

    /**
     * 存储教师监考会话
     * Key: publishId (考试发布ID), Value: Map<userId, WebSocketSession>
     */
    private static final Map<Long, Map<Long, WebSocketSession>> PROCTOR_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 存储用户ID到其监控的publishId的映射
     * Key: userId, Value: publishId
     */
    private static final Map<Long, Long> USER_PUBLISH_MAP = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public ProctorWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        Long publishId = getPublishId(session);
        
        if (userId == null || publishId == null) {
            log.warn("监考WebSocket连接失败：缺少必要参数");
            session.close(CloseStatus.BAD_DATA.withReason("缺少userId或publishId参数"));
            return;
        }

        // 注册教师监考会话
        PROCTOR_SESSIONS.computeIfAbsent(publishId, k -> new ConcurrentHashMap<>())
                .put(userId, session);
        USER_PUBLISH_MAP.put(userId, publishId);

        log.info("教师 {} 开始监控考试 {}, 当前监控该考试的教师数: {}", 
                userId, publishId, PROCTOR_SESSIONS.get(publishId).size());

        // 发送连接成功消息
        sendMessage(session, WebSocketMessage.success("proctor_connected", "监考连接成功"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserId(session);
        String payload = message.getPayload();
        log.debug("收到教师 {} 的监考消息: {}", userId, payload);

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            // 处理心跳消息
            if ("ping".equals(wsMessage.getType())) {
                sendMessage(session, WebSocketMessage.success("pong", "心跳响应"));
                return;
            }

            // 可以扩展其他消息类型，如强制交卷指令等
            log.info("教师 {} 发送监考消息类型: {}", userId, wsMessage.getType());
            
        } catch (Exception e) {
            log.error("处理监考WebSocket消息失败", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserId(session);
        log.error("监考WebSocket传输错误，教师ID: {}", userId, exception);
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserId(session);
        removeSession(session);
        log.info("教师 {} 监考WebSocket连接关闭，状态: {}", userId, status);
    }

    /**
     * 向监控指定考试的所有教师推送消息
     *
     * @param publishId 考试发布ID
     * @param message   消息内容
     */
    public void broadcastToProctors(Long publishId, WebSocketMessage message) {
        Map<Long, WebSocketSession> sessions = PROCTOR_SESSIONS.get(publishId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("考试 {} 当前无教师监控", publishId);
            return;
        }

        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            log.error("序列化监考消息失败", e);
            return;
        }

        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                    log.debug("向教师 {} 推送监考消息成功", userId);
                } catch (IOException e) {
                    log.error("向教师 {} 推送监考消息失败", userId, e);
                }
            }
        });
    }

    /**
     * 向指定教师发送消息
     */
    public boolean sendMessageToProctor(Long publishId, Long userId, WebSocketMessage message) {
        Map<Long, WebSocketSession> sessions = PROCTOR_SESSIONS.get(publishId);
        if (sessions == null) {
            return false;
        }
        WebSocketSession session = sessions.get(userId);
        return sendMessage(session, message);
    }

    /**
     * 获取监控指定考试的教师数量
     */
    public int getProctorCount(Long publishId) {
        Map<Long, WebSocketSession> sessions = PROCTOR_SESSIONS.get(publishId);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * 获取监控指定考试的所有教师ID
     */
    public Set<Long> getProctorIds(Long publishId) {
        Map<Long, WebSocketSession> sessions = PROCTOR_SESSIONS.get(publishId);
        return sessions == null ? Set.of() : sessions.keySet();
    }

    private boolean sendMessage(WebSocketSession session, WebSocketMessage message) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            return true;
        } catch (IOException e) {
            log.error("发送监考消息失败", e);
            return false;
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? (Long) userId : null;
    }

    private Long getPublishId(WebSocketSession session) {
        Object publishId = session.getAttributes().get("publishId");
        return publishId != null ? (Long) publishId : null;
    }

    private void removeSession(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) return;

        Long publishId = USER_PUBLISH_MAP.remove(userId);
        if (publishId != null) {
            Map<Long, WebSocketSession> sessions = PROCTOR_SESSIONS.get(publishId);
            if (sessions != null) {
                sessions.remove(userId);
                if (sessions.isEmpty()) {
                    PROCTOR_SESSIONS.remove(publishId);
                }
            }
        }
    }
}

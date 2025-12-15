package com.university.exam.config;

import com.university.exam.websocket.NoticeWebSocketHandler;
import com.university.exam.websocket.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置WebSocket连接端点和处理器
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NoticeWebSocketHandler noticeWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(NoticeWebSocketHandler noticeWebSocketHandler,
                          WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.noticeWebSocketHandler = noticeWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(noticeWebSocketHandler, "/ws/notice")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}

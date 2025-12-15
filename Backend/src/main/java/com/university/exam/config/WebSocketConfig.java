package com.university.exam.config;

import com.university.exam.websocket.NoticeWebSocketHandler;
import com.university.exam.websocket.ProctorHandshakeInterceptor;
import com.university.exam.websocket.ProctorWebSocketHandler;
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
    private final ProctorWebSocketHandler proctorWebSocketHandler;
    private final ProctorHandshakeInterceptor proctorHandshakeInterceptor;

    public WebSocketConfig(NoticeWebSocketHandler noticeWebSocketHandler,
                          WebSocketHandshakeInterceptor handshakeInterceptor,
                          ProctorWebSocketHandler proctorWebSocketHandler,
                          ProctorHandshakeInterceptor proctorHandshakeInterceptor) {
        this.noticeWebSocketHandler = noticeWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.proctorWebSocketHandler = proctorWebSocketHandler;
        this.proctorHandshakeInterceptor = proctorHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 通知WebSocket端点
        registry.addHandler(noticeWebSocketHandler, "/ws/notice")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
        
        // 监考WebSocket端点
        registry.addHandler(proctorWebSocketHandler, "/ws/proctor")
                .addInterceptors(proctorHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}

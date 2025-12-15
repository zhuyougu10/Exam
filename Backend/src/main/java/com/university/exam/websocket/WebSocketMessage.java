package com.university.exam.websocket;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket消息实体类
 * 用于WebSocket通信的消息封装
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型
     * - connected: 连接成功
     * - pong: 心跳响应
     * - notice: 系统通知
     * - exam_notice: 考试通知
     * - read_notice: 已读通知回执
     */
    private String type;

    /**
     * 消息内容/描述
     */
    private String message;

    /**
     * 消息数据（可选）
     */
    private Object data;

    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 创建成功消息
     */
    public static WebSocketMessage success(String type, String message) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType(type);
        wsMessage.setMessage(message);
        wsMessage.setSuccess(true);
        wsMessage.setTimestamp(LocalDateTime.now());
        return wsMessage;
    }

    /**
     * 创建成功消息（带数据）
     */
    public static WebSocketMessage success(String type, String message, Object data) {
        WebSocketMessage wsMessage = success(type, message);
        wsMessage.setData(data);
        return wsMessage;
    }

    /**
     * 创建通知消息
     */
    public static WebSocketMessage notice(Object noticeData) {
        return success("notice", "您有新的系统通知", noticeData);
    }

    /**
     * 创建考试通知消息
     */
    public static WebSocketMessage examNotice(Object noticeData) {
        return success("exam_notice", "您有新的考试通知", noticeData);
    }

    /**
     * 创建错误消息
     */
    public static WebSocketMessage error(String type, String message) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType(type);
        wsMessage.setMessage(message);
        wsMessage.setSuccess(false);
        wsMessage.setTimestamp(LocalDateTime.now());
        return wsMessage;
    }

    /**
     * 创建AI任务进度更新消息
     */
    public static WebSocketMessage taskProgress(Object taskData) {
        return success("task_progress", "任务进度更新", taskData);
    }

    /**
     * 创建AI任务完成消息
     */
    public static WebSocketMessage taskComplete(Object taskData) {
        return success("task_complete", "任务已完成", taskData);
    }

    /**
     * 创建文件状态更新消息
     */
    public static WebSocketMessage fileStatus(Object fileData) {
        return success("file_status", "文件状态更新", fileData);
    }
}

package com.university.exam.websocket;

import com.university.exam.entity.Notice;
import com.university.exam.entity.UserNotice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebSocket服务类
 * 提供WebSocket消息推送的业务逻辑
 *
 * @author exam-system
 * @since 2025-12-15
 */
@Slf4j
@Service
public class WebSocketService {

    private final NoticeWebSocketHandler webSocketHandler;

    public WebSocketService(NoticeWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 推送通知给指定用户
     *
     * @param userId 用户ID
     * @param notice 通知内容
     */
    public void pushNoticeToUser(Long userId, Notice notice) {
        NoticeVo noticeVo = convertToVo(notice);
        WebSocketMessage message = notice.getType() == 2 
            ? WebSocketMessage.examNotice(noticeVo)
            : WebSocketMessage.notice(noticeVo);
        
        boolean sent = webSocketHandler.sendMessage(userId, message);
        if (sent) {
            log.info("通知推送成功，用户ID: {}，通知ID: {}", userId, notice.getId());
        } else {
            log.debug("用户 {} 不在线，通知 {} 将在下次登录时获取", userId, notice.getId());
        }
    }

    /**
     * 推送通知给多个用户
     *
     * @param userIds 用户ID列表
     * @param notice 通知内容
     */
    public void pushNoticeToUsers(List<Long> userIds, Notice notice) {
        NoticeVo noticeVo = convertToVo(notice);
        WebSocketMessage message = notice.getType() == 2 
            ? WebSocketMessage.examNotice(noticeVo)
            : WebSocketMessage.notice(noticeVo);
        
        webSocketHandler.sendMessageToUsers(userIds, message);
        log.info("批量推送通知完成，通知ID: {}，目标用户数: {}", notice.getId(), userIds.size());
    }

    /**
     * 广播通知给所有在线用户
     *
     * @param notice 通知内容
     */
    public void broadcastNotice(Notice notice) {
        NoticeVo noticeVo = convertToVo(notice);
        WebSocketMessage message = notice.getType() == 2 
            ? WebSocketMessage.examNotice(noticeVo)
            : WebSocketMessage.notice(noticeVo);
        
        webSocketHandler.broadcastMessage(message);
        log.info("广播通知完成，通知ID: {}", notice.getId());
    }

    /**
     * 推送未读通知数量更新
     *
     * @param userId 用户ID
     * @param unreadCount 未读数量
     */
    public void pushUnreadCount(Long userId, int unreadCount) {
        WebSocketMessage message = WebSocketMessage.success("unread_count", "未读消息数量更新", unreadCount);
        webSocketHandler.sendMessage(userId, message);
    }

    /**
     * 推送AI任务进度更新
     *
     * @param userId 用户ID
     * @param taskData 任务数据
     */
    public void pushTaskProgress(Long userId, Object taskData) {
        WebSocketMessage message = WebSocketMessage.taskProgress(taskData);
        webSocketHandler.sendMessage(userId, message);
        log.debug("推送任务进度更新，用户ID: {}", userId);
    }

    /**
     * 推送AI任务完成通知
     *
     * @param userId 用户ID
     * @param taskData 任务数据
     */
    public void pushTaskComplete(Long userId, Object taskData) {
        WebSocketMessage message = WebSocketMessage.taskComplete(taskData);
        webSocketHandler.sendMessage(userId, message);
        log.info("推送任务完成通知，用户ID: {}", userId);
    }

    /**
     * 推送文件状态更新
     *
     * @param userId 用户ID
     * @param fileData 文件数据
     */
    public void pushFileStatus(Long userId, Object fileData) {
        WebSocketMessage message = WebSocketMessage.fileStatus(fileData);
        webSocketHandler.sendMessage(userId, message);
        log.debug("推送文件状态更新，用户ID: {}", userId);
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        return webSocketHandler.isUserOnline(userId);
    }

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return webSocketHandler.getOnlineUserCount();
    }

    /**
     * 将Notice实体转换为VO
     */
    private NoticeVo convertToVo(Notice notice) {
        NoticeVo vo = new NoticeVo();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setType(notice.getType());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }

    /**
     * 通知视图对象
     */
    @lombok.Data
    public static class NoticeVo {
        private Long id;
        private String title;
        private String content;
        private Byte type;
        private java.time.LocalDateTime createTime;
        private Boolean isRead = false;
    }
}

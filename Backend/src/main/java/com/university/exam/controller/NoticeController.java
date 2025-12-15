package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.entity.Notice;
import com.university.exam.entity.UserNotice;
import com.university.exam.service.NoticeService;
import com.university.exam.service.UserNoticeService;
import com.university.exam.websocket.WebSocketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统通知控制器
 * 提供通知的增删改查和实时推送功能
 *
 * @author exam-system
 * @since 2025-12-15
 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;
    private final UserNoticeService userNoticeService;
    private final WebSocketService webSocketService;
    private final JwtUtils jwtUtils;

    public NoticeController(NoticeService noticeService, 
                           UserNoticeService userNoticeService,
                           WebSocketService webSocketService,
                           JwtUtils jwtUtils) {
        this.noticeService = noticeService;
        this.userNoticeService = userNoticeService;
        this.webSocketService = webSocketService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/my-list")
    public Result<Map<String, Object>> getMyNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Byte type,
            @RequestParam(required = false) Boolean isRead,
            HttpServletRequest request) {
        
        Long userId = getUserId(request);
        
        // 查询用户的通知关联记录
        LambdaQueryWrapper<UserNotice> userNoticeWrapper = new LambdaQueryWrapper<>();
        userNoticeWrapper.eq(UserNotice::getUserId, userId);
        if (isRead != null) {
            userNoticeWrapper.eq(UserNotice::getIsRead, isRead ? (byte) 1 : (byte) 0);
        }
        userNoticeWrapper.orderByDesc(UserNotice::getCreateTime);
        
        Page<UserNotice> userNoticePage = userNoticeService.page(new Page<>(page, size), userNoticeWrapper);
        
        // 获取通知ID列表
        List<Long> noticeIds = userNoticePage.getRecords().stream()
                .map(UserNotice::getNoticeId)
                .collect(Collectors.toList());
        
        if (noticeIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("records", List.of());
            result.put("total", 0);
            result.put("unreadCount", 0);
            return Result.success(result);
        }
        
        // 查询通知详情
        LambdaQueryWrapper<Notice> noticeWrapper = new LambdaQueryWrapper<>();
        noticeWrapper.in(Notice::getId, noticeIds);
        if (type != null) {
            noticeWrapper.eq(Notice::getType, type);
        }
        List<Notice> notices = noticeService.list(noticeWrapper);
        
        // 构建已读状态映射
        Map<Long, UserNotice> userNoticeMap = userNoticePage.getRecords().stream()
                .collect(Collectors.toMap(UserNotice::getNoticeId, un -> un));
        
        // 组装返回数据
        List<NoticeVo> voList = notices.stream().map(notice -> {
            NoticeVo vo = new NoticeVo();
            vo.setId(notice.getId());
            vo.setTitle(notice.getTitle());
            vo.setContent(notice.getContent());
            vo.setType(notice.getType());
            vo.setCreateTime(notice.getCreateTime());
            UserNotice un = userNoticeMap.get(notice.getId());
            vo.setIsRead(un != null && un.getIsRead() == 1);
            vo.setReadTime(un != null ? un.getReadTime() : null);
            return vo;
        }).collect(Collectors.toList());
        
        // 查询未读数量
        long unreadCount = userNoticeService.count(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getIsRead, (byte) 0));
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", userNoticePage.getTotal());
        result.put("unreadCount", unreadCount);
        return Result.success(result);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        
        long count = userNoticeService.count(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getIsRead, (byte) 0));
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/read/{noticeId}")
    public Result<Map<String, Object>> markAsRead(@PathVariable Long noticeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        
        UserNotice userNotice = userNoticeService.getOne(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getNoticeId, noticeId));
        
        if (userNotice != null && userNotice.getIsRead() == 0) {
            userNotice.setIsRead((byte) 1);
            userNotice.setReadTime(LocalDateTime.now());
            userNotice.setUpdateTime(LocalDateTime.now());
            userNoticeService.updateById(userNotice);
        }
        
        // 返回更新后的未读数量
        long unreadCount = userNoticeService.count(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getIsRead, (byte) 0));
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("unreadCount", unreadCount);
        return Result.success(result);
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public Result<Map<String, Object>> markAllAsRead(HttpServletRequest request) {
        Long userId = getUserId(request);
        
        List<UserNotice> unreadList = userNoticeService.list(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getIsRead, (byte) 0));
        
        LocalDateTime now = LocalDateTime.now();
        for (UserNotice un : unreadList) {
            un.setIsRead((byte) 1);
            un.setReadTime(now);
            un.setUpdateTime(now);
        }
        
        if (!unreadList.isEmpty()) {
            userNoticeService.updateBatchById(unreadList);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("unreadCount", 0);
        return Result.success(result);
    }

    /**
     * 发布通知（管理员/教师）
     */
    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole(3, 2)")
    public Result<Map<String, Object>> publishNotice(@RequestBody PublishNoticeRequest req, HttpServletRequest request) {
        Long senderId = getUserId(request);
        
        // 创建通知
        Notice notice = new Notice();
        notice.setTitle(req.getTitle());
        notice.setContent(req.getContent());
        notice.setType(req.getType());
        notice.setTargetType(req.getTargetType());
        notice.setSenderId(senderId);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        notice.setCreateBy(senderId);
        notice.setUpdateBy(senderId);
        notice.setIsDeleted((byte) 0);
        
        noticeService.save(notice);
        
        // 根据目标类型创建用户通知记录并推送
        if (req.getTargetType() == 1) {
            // 全体用户 - 广播推送
            webSocketService.broadcastNotice(notice);
        } else if (req.getTargetType() == 3 && req.getTargetUserIds() != null) {
            // 指定用户
            LocalDateTime now = LocalDateTime.now();
            for (Long targetUserId : req.getTargetUserIds()) {
                UserNotice userNotice = new UserNotice();
                userNotice.setNoticeId(notice.getId());
                userNotice.setUserId(targetUserId);
                userNotice.setIsRead((byte) 0);
                userNotice.setCreateTime(now);
                userNotice.setUpdateTime(now);
                userNotice.setCreateBy(senderId);
                userNotice.setUpdateBy(senderId);
                userNotice.setIsDeleted((byte) 0);
                userNoticeService.save(userNotice);
            }
            // 推送给指定用户
            webSocketService.pushNoticeToUsers(req.getTargetUserIds(), notice);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("noticeId", notice.getId());
        return Result.success(result);
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{noticeId}")
    public Result<Notice> getNoticeDetail(@PathVariable Long noticeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        
        // 自动标记为已读
        UserNotice userNotice = userNoticeService.getOne(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getNoticeId, noticeId));
        
        if (userNotice != null && userNotice.getIsRead() == 0) {
            userNotice.setIsRead((byte) 1);
            userNotice.setReadTime(LocalDateTime.now());
            userNotice.setUpdateTime(LocalDateTime.now());
            userNoticeService.updateById(userNotice);
            
            // 推送未读数量更新
            long unreadCount = userNoticeService.count(new LambdaQueryWrapper<UserNotice>()
                    .eq(UserNotice::getUserId, userId)
                    .eq(UserNotice::getIsRead, (byte) 0));
            webSocketService.pushUnreadCount(userId, (int) unreadCount);
        }
        
        return Result.success(noticeService.getById(noticeId));
    }

    /**
     * 删除通知（管理员）
     */
    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole(3)")
    public Result<Map<String, Object>> deleteNotice(@PathVariable Long noticeId) {
        noticeService.removeById(noticeId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return Result.success(result);
    }

    /**
     * 获取WebSocket在线状态
     */
    @GetMapping("/ws/status")
    public Result<Map<String, Object>> getWebSocketStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("onlineCount", webSocketService.getOnlineUserCount());
        return Result.success(result);
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 通知视图对象
     */
    @Data
    public static class NoticeVo {
        private Long id;
        private String title;
        private String content;
        private Byte type;
        private LocalDateTime createTime;
        private Boolean isRead;
        private LocalDateTime readTime;
    }

    /**
     * 发布通知请求
     */
    @Data
    public static class PublishNoticeRequest {
        private String title;
        private String content;
        private Byte type;
        private Byte targetType;
        private List<Long> targetUserIds;
    }
}

package com.university.exam.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.Notice;
import com.university.exam.entity.Publish;
import com.university.exam.entity.User;
import com.university.exam.entity.UserNotice;
import com.university.exam.mapper.PublishMapper;
import com.university.exam.service.NoticeService;
import com.university.exam.service.PublishService;
import com.university.exam.service.UserNoticeService;
import com.university.exam.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 考试发布/场次表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends ServiceImpl<PublishMapper, Publish> implements PublishService {

    private final NoticeService noticeService;
    private final UserNoticeService userNoticeService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishExam(PublishRequest req, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 时间校验
        // 结束时间必须晚于开始时间
        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BizException(400, "开始时间不能晚于结束时间");
        }
        
        // 开始时间不能早于当前时间 (允许1分钟误差)
        if (req.getStartTime().isBefore(now.minusMinutes(1))) {
            throw new BizException(400, "考试开始时间不能早于当前时间");
        }

        Publish publish = new Publish();
        publish.setPaperId(req.getPaperId());
        publish.setTitle(req.getTitle());
        publish.setTargetDeptIds(JSONUtil.toJsonStr(req.getTargetDeptIds()));
        publish.setStartTime(req.getStartTime());
        publish.setEndTime(req.getEndTime());
        publish.setLimitCount(req.getLimitCount() != null ? req.getLimitCount() : 1);
        publish.setPassword(req.getPassword());
        
        // 状态逻辑：如果开始时间在未来，则是未开始(0)，否则进行中(1)
        byte status = now.isBefore(req.getStartTime()) ? (byte)0 : (byte)1;
        publish.setStatus(status);
        
        publish.setCreateBy(userId);
        publish.setUpdateBy(userId);
        publish.setCreateTime(now);
        publish.setUpdateTime(now);

        this.save(publish);

        // 异步发送通知
        sendExamNotification(publish, req.getTargetDeptIds(), userId);

        return publish.getId();
    }

    @Override
    public boolean checkIsPublished(Long paperId) {
        return this.count(new LambdaQueryWrapper<Publish>()
                .eq(Publish::getPaperId, paperId)) > 0;
    }

    /**
     * 异步发送考试通知
     */
    @Async
    public void sendExamNotification(Publish publish, List<Long> deptIds, Long senderId) {
        // 1. 创建系统通知
        Notice notice = new Notice();
        notice.setTitle("考试通知: " + publish.getTitle());
        String timeRange = publish.getStartTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) 
                + " 至 " + publish.getEndTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        notice.setContent(String.format("您有一场新的考试安排。\n考试名称：%s\n考试时间：%s\n请准时参加。", 
                publish.getTitle(), timeRange));
        notice.setType((byte) 2); // 考试通知
        notice.setTargetType((byte) 2); // 指定部门
        notice.setSenderId(senderId);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        noticeService.save(notice);

        // 2. 查找目标部门下的所有学生
        if (deptIds != null && !deptIds.isEmpty()) {
            List<User> students = userService.list(new LambdaQueryWrapper<User>()
                    .in(User::getDeptId, deptIds)
                    .eq(User::getRole, 1)); // 1-学生

            // 3. 批量创建用户消息关联
            List<UserNotice> userNotices = new ArrayList<>();
            for (User student : students) {
                UserNotice un = new UserNotice();
                un.setUserId(student.getId());
                un.setNoticeId(notice.getId());
                un.setIsRead((byte) 0);
                un.setCreateTime(LocalDateTime.now());
                un.setUpdateTime(LocalDateTime.now());
                userNotices.add(un);
            }
            if (!userNotices.isEmpty()) {
                userNoticeService.saveBatch(userNotices);
            }
        }
    }
}
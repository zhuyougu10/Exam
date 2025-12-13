package com.university.exam.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.dto.student.StudentExamDto;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.*;
import com.university.exam.mapper.PublishMapper;
import com.university.exam.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PaperService paperService;
    private final RecordService recordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishExam(PublishRequest req, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BizException(400, "开始时间不能晚于结束时间");
        }
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
        
        byte status = now.isBefore(req.getStartTime()) ? (byte)0 : (byte)1;
        publish.setStatus(status);
        
        publish.setCreateBy(userId);
        publish.setUpdateBy(userId);
        publish.setCreateTime(now);
        publish.setUpdateTime(now);

        this.save(publish);

        sendExamNotification(publish, req.getTargetDeptIds(), userId);

        return publish.getId();
    }

    @Override
    public boolean checkIsPublished(Long paperId) {
        return this.count(new LambdaQueryWrapper<Publish>()
                .eq(Publish::getPaperId, paperId)) > 0;
    }

    /**
     * 获取学生考试列表实现逻辑
     */
    @Override
    public List<StudentExamDto> listStudentExams(Long userId, Long deptId) {
        // 1. 查询所有已发布(1)或未开始(0)的考试
        // 这里简化逻辑，查询所有未逻辑删除的记录，然后在内存中过滤
        // 实际生产中应配合 ElasticSearch 或 MySQL JSON 查询优化
        List<Publish> allPublishes = this.list(new LambdaQueryWrapper<Publish>()
                .in(Publish::getStatus, 0, 1, 2)
                .orderByDesc(Publish::getStartTime));

        List<StudentExamDto> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Publish publish : allPublishes) {
            // 2. 权限过滤：检查班级权限
            boolean hasAccess = false;
            try {
                List<Long> targetIds = JSONUtil.toList(publish.getTargetDeptIds(), Long.class);
                if (deptId != null && CollUtil.contains(targetIds, deptId)) {
                    hasAccess = true;
                }
            } catch (Exception e) {
                // ignore parsing error
            }

            if (!hasAccess) continue;

            // 3. 构建 DTO
            StudentExamDto dto = new StudentExamDto();
            dto.setId(publish.getId());
            dto.setTitle(publish.getTitle());
            dto.setStartTime(publish.getStartTime());
            dto.setEndTime(publish.getEndTime());
            dto.setLimitCount(publish.getLimitCount());
            dto.setPaperId(publish.getPaperId());

            // 获取试卷时长
            Paper paper = paperService.getById(publish.getPaperId());
            dto.setDuration(paper != null ? paper.getDuration() : 0);

            // 4. 计算状态
            // 默认状态逻辑
            int status = 0; // 未开始
            if (now.isAfter(publish.getStartTime()) && now.isBefore(publish.getEndTime())) {
                status = 1; // 进行中
            } else if (now.isAfter(publish.getEndTime())) {
                status = 2; // 已过期
            }

            // 查询用户已考次数
            long triedCount = recordService.count(new LambdaQueryWrapper<Record>()
                    .eq(Record::getUserId, userId)
                    .eq(Record::getPublishId, publish.getId()));
            dto.setTriedCount((int) triedCount);

            // 如果次数已用完，视为结束
            if (publish.getLimitCount() > 0 && triedCount >= publish.getLimitCount()) {
                status = 2; 
            }
            
            // 检查是否有"进行中"的记录 (断点续考)
            long ongoingCount = recordService.count(new LambdaQueryWrapper<Record>()
                    .eq(Record::getUserId, userId)
                    .eq(Record::getPublishId, publish.getId())
                    .eq(Record::getStatus, 1));
            if (ongoingCount > 0) {
                status = 1; // 强制设为进行中，允许进入
            }

            dto.setStatus(status);
            result.add(dto);
        }

        return result;
    }

    @Async
    public void sendExamNotification(Publish publish, List<Long> deptIds, Long senderId) {
        Notice notice = new Notice();
        notice.setTitle("考试通知: " + publish.getTitle());
        String timeRange = publish.getStartTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) 
                + " 至 " + publish.getEndTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        notice.setContent(String.format("您有一场新的考试安排。\n考试名称：%s\n考试时间：%s\n请准时参加。", 
                publish.getTitle(), timeRange));
        notice.setType((byte) 2);
        notice.setTargetType((byte) 2);
        notice.setSenderId(senderId);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        noticeService.save(notice);

        if (deptIds != null && !deptIds.isEmpty()) {
            List<User> students = userService.list(new LambdaQueryWrapper<User>()
                    .in(User::getDeptId, deptIds)
                    .eq(User::getRole, 1));

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
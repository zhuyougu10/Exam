package com.university.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.university.exam.common.dto.student.ProctorLogDto;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.ProctorLog;
import com.university.exam.entity.Record;
import com.university.exam.entity.User;
import com.university.exam.mapper.ProctorLogMapper;
import com.university.exam.mapper.RecordMapper;
import com.university.exam.mapper.UserMapper;
import com.university.exam.service.ProctorLogService;
import com.university.exam.websocket.ProctorWebSocketHandler;
import com.university.exam.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监考日志表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProctorLogServiceImpl extends ServiceImpl<ProctorLogMapper, ProctorLog> implements ProctorLogService {

    private final RecordMapper recordMapper;
    private final UserMapper userMapper;
    private final ProctorWebSocketHandler proctorWebSocketHandler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleProctorLog(ProctorLogDto dto, Long userId) {
        // 1. 校验归属权
        Record record = recordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BizException(404, "考试记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            log.warn("非法监考日志上报: userId={}, recordId={}", userId, dto.getRecordId());
            throw new BizException(403, "非法操作: 无法上报他人的考试日志");
        }
        
        // 2. 只有"进行中"的考试才记录日志 (避免交卷后继续上报)
        if (record.getStatus() != 1) {
            // 这里不抛异常，避免前端报错影响用户体验，只需忽略即可
            log.info("忽略非进行中考试的日志上报: recordId={}, status={}", record.getId(), record.getStatus());
            return;
        }

        // 3. 保存日志
        ProctorLog logEntry = new ProctorLog();
        logEntry.setRecordId(dto.getRecordId());
        logEntry.setActionType(dto.getActionType());
        logEntry.setImgSnapshot(dto.getImgSnapshot());
        logEntry.setContent(dto.getContent());
        logEntry.setHappenTime(LocalDateTime.now());
        
        logEntry.setCreateBy(userId);
        logEntry.setUpdateBy(userId);
        logEntry.setCreateTime(LocalDateTime.now());
        logEntry.setUpdateTime(LocalDateTime.now());
        
        this.save(logEntry);

        // 4. 简单异常判定 (示例逻辑)
        if ("switch_screen".equals(dto.getActionType()) || "leave_page".equals(dto.getActionType())) {
            // 统计该考生该场考试的切屏次数
            long count = this.count(new LambdaQueryWrapper<ProctorLog>()
                    .eq(ProctorLog::getRecordId, dto.getRecordId())
                    .in(ProctorLog::getActionType, "switch_screen", "leave_page"));
            
            log.info("考生[{}] 切屏/离开页面警告, 当前次数: {}", userId, count);
            
            // 如果需要严格模式，可以在此处判断 count > 3 自动强制交卷
            // 暂时只记录，不做强制动作
        }

        // 5. 通过WebSocket实时推送给监考教师
        pushLogToProctors(record.getPublishId(), logEntry, userId);
    }

    /**
     * 通过WebSocket实时推送监考日志给教师
     */
    private void pushLogToProctors(Long publishId, ProctorLog logEntry, Long studentId) {
        try {
            User student = userMapper.selectById(studentId);
            
            Map<String, Object> logData = new HashMap<>();
            logData.put("id", logEntry.getId());
            logData.put("recordId", logEntry.getRecordId());
            logData.put("studentId", studentId);
            logData.put("studentName", student != null ? student.getRealName() : "未知");
            logData.put("actionType", logEntry.getActionType());
            logData.put("actionLabel", getActionLabel(logEntry.getActionType()));
            logData.put("content", logEntry.getContent());
            logData.put("imgSnapshot", logEntry.getImgSnapshot());
            logData.put("happenTime", logEntry.getHappenTime());
            
            // 统计该学生的异常次数
            long warningCount = this.count(new LambdaQueryWrapper<ProctorLog>()
                    .eq(ProctorLog::getRecordId, logEntry.getRecordId())
                    .in(ProctorLog::getActionType, "switch_screen", "leave_page", "env_abnormal"));
            logData.put("warningCount", warningCount);

            WebSocketMessage message = WebSocketMessage.proctorLog(logData);
            proctorWebSocketHandler.broadcastToProctors(publishId, message);
            
            log.debug("已推送监考日志到考试 {} 的监考教师", publishId);
        } catch (Exception e) {
            log.error("推送监考日志失败", e);
        }
    }

    private String getActionLabel(String actionType) {
        return switch (actionType) {
            case "switch_screen" -> "切换屏幕";
            case "leave_page" -> "离开页面";
            case "env_abnormal" -> "环境异常";
            case "snapshot" -> "定时抓拍";
            default -> actionType;
        };
    }

    @Override
    public List<Map<String, Object>> getProctorLogsByPublishId(Long publishId) {
        // 获取该考试的所有记录ID
        List<Record> records = recordMapper.selectList(
                new LambdaQueryWrapper<Record>().eq(Record::getPublishId, publishId));
        
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> recordIds = records.stream().map(Record::getId).toList();
        
        // 获取所有监考日志
        List<ProctorLog> logs = this.list(new LambdaQueryWrapper<ProctorLog>()
                .in(ProctorLog::getRecordId, recordIds)
                .orderByDesc(ProctorLog::getHappenTime));

        // 构建recordId -> userId映射
        Map<Long, Long> recordUserMap = new HashMap<>();
        records.forEach(r -> recordUserMap.put(r.getId(), r.getUserId()));

        // 获取所有相关用户
        List<Long> userIds = records.stream().map(Record::getUserId).distinct().toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProctorLog logItem : logs) {
            Long userId = recordUserMap.get(logItem.getRecordId());
            User user = userMap.get(userId);

            Map<String, Object> item = new HashMap<>();
            item.put("id", logItem.getId());
            item.put("recordId", logItem.getRecordId());
            item.put("studentId", userId);
            item.put("studentName", user != null ? user.getRealName() : "未知");
            item.put("actionType", logItem.getActionType());
            item.put("actionLabel", getActionLabel(logItem.getActionType()));
            item.put("content", logItem.getContent());
            item.put("imgSnapshot", logItem.getImgSnapshot());
            item.put("happenTime", logItem.getHappenTime());
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getExamStudentStatus(Long publishId) {
        // 获取该考试的所有记录
        List<Record> records = recordMapper.selectList(
                new LambdaQueryWrapper<Record>().eq(Record::getPublishId, publishId));

        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有相关用户
        List<Long> userIds = records.stream().map(Record::getUserId).distinct().toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // 获取每个记录的警告次数
        List<Long> recordIds = records.stream().map(Record::getId).toList();
        Map<Long, Long> warningCountMap = new HashMap<>();
        for (Long recordId : recordIds) {
            long count = this.count(new LambdaQueryWrapper<ProctorLog>()
                    .eq(ProctorLog::getRecordId, recordId)
                    .in(ProctorLog::getActionType, "switch_screen", "leave_page", "env_abnormal"));
            warningCountMap.put(recordId, count);
        }

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Record record : records) {
            User user = userMap.get(record.getUserId());

            Map<String, Object> item = new HashMap<>();
            item.put("recordId", record.getId());
            item.put("studentId", record.getUserId());
            item.put("studentName", user != null ? user.getRealName() : "未知");
            item.put("status", record.getStatus());
            item.put("statusLabel", getStatusLabel(record.getStatus()));
            item.put("startTime", record.getStartTime());
            item.put("submitTime", record.getSubmitTime());
            item.put("warningCount", warningCountMap.getOrDefault(record.getId(), 0L));
            item.put("userIp", record.getUserIp());
            result.add(item);
        }

        return result;
    }

    private String getStatusLabel(Byte status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "未开始";
            case 1 -> "进行中";
            case 2 -> "已交卷";
            case 3 -> "已批卷";
            default -> "未知";
        };
    }

    @Override
    public Map<String, Object> getProctorStats(Long publishId) {
        List<Record> records = recordMapper.selectList(
                new LambdaQueryWrapper<Record>().eq(Record::getPublishId, publishId));

        int total = records.size();
        int inProgress = 0;
        int submitted = 0;
        int notStarted = 0;

        for (Record record : records) {
            if (record.getStatus() == null) continue;
            switch (record.getStatus()) {
                case 0 -> notStarted++;
                case 1 -> inProgress++;
                case 2, 3 -> submitted++;
            }
        }

        // 统计总警告次数
        List<Long> recordIds = records.stream().map(Record::getId).toList();
        long totalWarnings = 0;
        if (!recordIds.isEmpty()) {
            totalWarnings = this.count(new LambdaQueryWrapper<ProctorLog>()
                    .in(ProctorLog::getRecordId, recordIds)
                    .in(ProctorLog::getActionType, "switch_screen", "leave_page", "env_abnormal"));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("inProgress", inProgress);
        stats.put("submitted", submitted);
        stats.put("notStarted", notStarted);
        stats.put("totalWarnings", totalWarnings);
        stats.put("proctorCount", proctorWebSocketHandler.getProctorCount(publishId));

        return stats;
    }
}
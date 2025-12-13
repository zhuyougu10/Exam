package com.university.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.university.exam.common.dto.student.ProctorLogDto;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.ProctorLog;
import com.university.exam.entity.Record;
import com.university.exam.mapper.ProctorLogMapper;
import com.university.exam.mapper.RecordMapper;
import com.university.exam.service.ProctorLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    }
}
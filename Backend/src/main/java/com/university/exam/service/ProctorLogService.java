package com.university.exam.service;

import com.university.exam.common.dto.student.ProctorLogDto;
import com.university.exam.entity.ProctorLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 监考日志表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface ProctorLogService extends IService<ProctorLog> {

    /**
     * 处理考生上报的监考日志
     * @param dto 日志数据
     * @param userId 当前用户ID
     */
    void handleProctorLog(ProctorLogDto dto, Long userId);
}
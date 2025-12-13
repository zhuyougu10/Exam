package com.university.exam.controller;

import com.university.exam.common.dto.student.ProctorLogDto;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.service.ProctorLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监考管理控制器
 * 处理监考日志上报等操作
 *
 * @author MySQL数据库架构师
 * @since 2025-12-13
 */
@RestController
@RequestMapping("/api/proctor")
@RequiredArgsConstructor
public class ProctorController {

    private final ProctorLogService proctorLogService;
    private final JwtUtils jwtUtils;

    /**
     * 上报监考日志
     * POST /api/proctor/log
     *
     * @param dto     日志数据
     * @param request HTTP请求
     * @return 成功响应
     */
    @PostMapping("/log")
    public Result<?> reportLog(@Valid @RequestBody ProctorLogDto dto, HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);

        proctorLogService.handleProctorLog(dto, userId);
        return Result.success(null, "日志上报成功");
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
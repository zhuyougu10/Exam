package com.university.exam.controller;

import com.university.exam.common.dto.student.DashboardStatsVo;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.service.RecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考生端-仪表盘接口
 *
 * @author MySQL数据库架构师
 * @since 2025-12-13
 */
@RestController
@RequestMapping("/api/student/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final RecordService recordService;
    private final JwtUtils jwtUtils;

    /**
     * 获取仪表盘统计数据
     * GET /api/student/dashboard/stats
     */
    @GetMapping("/stats")
    public Result<DashboardStatsVo> getDashboardStats(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);

        DashboardStatsVo stats = recordService.getStudentStats(userId);
        return Result.success(stats);
    }
}
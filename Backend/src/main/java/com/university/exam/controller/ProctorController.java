package com.university.exam.controller;

import com.university.exam.common.dto.student.ProctorLogDto;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.service.ProctorLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 监考管理控制器
 * 处理监考日志上报和教师监考查询
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
     * 上报监考日志（学生端）
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

    /**
     * 获取考试监考日志列表（教师端）
     * GET /api/proctor/logs?publishId=xxx
     *
     * @param publishId 考试发布ID
     * @return 监考日志列表
     */
    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> getProctorLogs(@RequestParam Long publishId) {
        List<Map<String, Object>> logs = proctorLogService.getProctorLogsByPublishId(publishId);
        return Result.success(logs);
    }

    /**
     * 获取考试学生状态列表（教师端）
     * GET /api/proctor/students?publishId=xxx
     *
     * @param publishId 考试发布ID
     * @return 学生状态列表
     */
    @GetMapping("/students")
    public Result<List<Map<String, Object>>> getExamStudents(@RequestParam Long publishId) {
        List<Map<String, Object>> students = proctorLogService.getExamStudentStatus(publishId);
        return Result.success(students);
    }

    /**
     * 获取考试监考统计（教师端）
     * GET /api/proctor/stats?publishId=xxx
     *
     * @param publishId 考试发布ID
     * @return 监考统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getProctorStats(@RequestParam Long publishId) {
        Map<String, Object> stats = proctorLogService.getProctorStats(publishId);
        return Result.success(stats);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
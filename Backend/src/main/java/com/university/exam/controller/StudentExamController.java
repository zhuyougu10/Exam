package com.university.exam.controller;

import com.university.exam.common.dto.student.ExamPaperVo;
import com.university.exam.common.dto.student.StudentExamDto;
import com.university.exam.common.dto.student.SubmitExamRequest;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.common.vo.StudentExamResultVo;
import com.university.exam.service.PublishService;
import com.university.exam.service.RecordService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考生端-考试接口
 *
 * @author MySQL数据库架构师
 * @since 2025-12-13
 */
@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class StudentExamController {

    private final PublishService publishService;
    private final RecordService recordService;
    private final JwtUtils jwtUtils;

    /**
     * 获取当前学生可参加的考试列表
     * GET /api/exam/my-list
     */
    @GetMapping("/my-list")
    public Result<List<StudentExamDto>> getMyExamList(HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        // 解析 deptId
        Claims claims = jwtUtils.parseToken(token);
        Long deptId = claims.get("deptId", Long.class);

        List<StudentExamDto> list = publishService.listStudentExams(userId, deptId);
        return Result.success(list);
    }

    /**
     * 开始考试
     * POST /api/exam/start/{publishId}
     */
    @PostMapping("/start/{publishId}")
    public Result<ExamPaperVo> startExam(@PathVariable Long publishId, HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);

        ExamPaperVo paperVo = recordService.startExam(userId, publishId);
        return Result.success(paperVo, "考试开始，请认真作答");
    }

    /**
     * 交卷
     * POST /api/exam/submit
     */
    @PostMapping("/submit")
    public Result<Void> submitExam(@RequestBody SubmitExamRequest submitRequest, HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);

        recordService.submitExam(submitRequest, userId);
        return Result.success(null, "交卷成功，系统正在后台阅卷");
    }

    /**
     * 获取考试结果详情 (通过 recordId)
     * GET /api/exam/result/{recordId}
     */
    @GetMapping("/result/{recordId}")
    public Result<StudentExamResultVo> getExamResult(@PathVariable Long recordId, HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);

        StudentExamResultVo vo = recordService.getStudentExamResult(recordId, userId);
        return Result.success(vo);
    }

    /**
     * 根据 publishId 获取最新的考试结果
     * GET /api/exam/result/publish/{publishId}
     */
    @GetMapping("/result/publish/{publishId}")
    public Result<StudentExamResultVo> getLatestExamResult(@PathVariable Long publishId, HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);

        Long recordId = recordService.getLatestRecordId(userId, publishId);
        if (recordId == null) {
             throw new BizException(404, "未找到该场考试的答题记录");
        }

        StudentExamResultVo vo = recordService.getStudentExamResult(recordId, userId);
        return Result.success(vo);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
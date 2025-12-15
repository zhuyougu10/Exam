package com.university.exam.controller;

import com.university.exam.common.dto.student.ExamPaperVo;
import com.university.exam.common.dto.student.StudentExamDto;
import com.university.exam.common.dto.student.SubmitExamRequest;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.common.vo.StudentExamResultVo;
import com.university.exam.entity.Publish;
import com.university.exam.entity.Record;
import com.university.exam.service.PublishService;
import com.university.exam.service.RecordService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     */
    @GetMapping("/my-list")
    public Result<List<StudentExamDto>> getMyExamList(HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        Claims claims = jwtUtils.parseToken(token);
        Long deptId = claims.get("deptId", Long.class);

        List<StudentExamDto> list = publishService.listStudentExams(userId, deptId);
        return Result.success(list);
    }

    /**
     * 验证考试密码
     */
    @PostMapping("/verify-password/{publishId}")
    public Result<Boolean> verifyPassword(@PathVariable Long publishId, @RequestBody java.util.Map<String, String> body) {
        Publish publish = publishService.getById(publishId);
        if (publish == null) {
            throw new BizException(404, "考试不存在");
        }
        
        String inputPassword = body.get("password");
        // 如果考试没有设置密码，直接通过
        if (publish.getPassword() == null || publish.getPassword().isEmpty()) {
            return Result.success(true, "无需密码");
        }
        
        // 验证密码
        if (publish.getPassword().equals(inputPassword)) {
            return Result.success(true, "密码正确");
        } else {
            throw new BizException(400, "密码错误，请重新输入");
        }
    }

    /**
     * 开始考试
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
     */
    @GetMapping("/result/{recordId}")
    public Result<StudentExamResultVo> getExamResult(@PathVariable Long recordId, HttpServletRequest request) {
        String token = getToken(request);
        Long userId = jwtUtils.getUserIdFromToken(token);

        StudentExamResultVo vo = recordService.getStudentExamResult(recordId, userId);
        
        // --- 新增逻辑: 校验是否允许查看详情 ---
        Record record = recordService.getById(recordId);
        if (record != null) {
            handleAnalysisPermission(vo, record.getPublishId());
        }
        // ------------------------------------

        return Result.success(vo);
    }

    /**
     * 根据 publishId 获取最新的考试结果
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
        
        // --- 新增逻辑: 校验是否允许查看详情 ---
        handleAnalysisPermission(vo, publishId);
        // ------------------------------------
        
        return Result.success(vo);
    }

    /**
     * 处理解析查看权限逻辑
     */
    private void handleAnalysisPermission(StudentExamResultVo vo, Long publishId) {
        Publish publish = publishService.getById(publishId);
        if (publish == null) return;

        // 1. 设置考试截止时间
        vo.setExamEndTime(publish.getEndTime().toString().replace("T", " "));

        // 2. 判断逻辑: 当前时间 > 结束时间 OR 允许提前看
        boolean isTimeUp = LocalDateTime.now().isAfter(publish.getEndTime());
        boolean isAllowed = publish.getAllowEarlyAnalysis() != null && publish.getAllowEarlyAnalysis() == 1;
        
        if (isTimeUp || isAllowed) {
            vo.setCanViewAnalysis(true);
        } else {
            // 不允许查看: 标记为 false，并清空所有题目数据
            // 确保前端无法通过抓包获取题目内容
            vo.setCanViewAnalysis(false);
            vo.setQuestionList(null); // 或者 Collections.emptyList()
        }
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
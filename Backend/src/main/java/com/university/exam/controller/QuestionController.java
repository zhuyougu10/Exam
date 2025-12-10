package com.university.exam.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.AiTask;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.Question;
import com.university.exam.service.AiTaskService;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.QuestionGenerationService;
import com.university.exam.service.QuestionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题库管理控制器
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-10
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionGenerationService generationService;
    private final AiTaskService aiTaskService;
    private final CourseUserService courseUserService;

    // ==================== AI 生成相关 ====================

    /**
     * 启动 AI 智能出题任务 (异步)
     * POST /api/question/ai-generate
     */
    @PostMapping("/question/ai-generate")
    @PreAuthorize("hasAnyRole(2, 3)") // 教师或管理员
    public Result<?> startAiGeneration(@RequestBody AiGenerateRequest req) {
        Long userId = getCurrentUserId();
        
        // 传递 type 参数
        Long taskId = generationService.startGenerationTask(
                req.getCourseId(),
                req.getTopic(),
                req.getTotalCount(),
                req.getDifficulty(),
                req.getType(), // 新增
                userId
        );
        
        return Result.success(Map.of("taskId", taskId), "AI 出题任务已启动");
    }

    /**
     * 查询任务列表/进度
     * GET /api/question/task/list
     */
    @GetMapping("/question/task/list")
    public Result<?> getTaskList(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        // 只能看自己的任务
        IPage<AiTask> taskPage = aiTaskService.page(new Page<>(page, size),
                new LambdaQueryWrapper<AiTask>()
                        .eq(AiTask::getCreateBy, userId)
                        .eq(AiTask::getType, 1) // 1-出题
                        .orderByDesc(AiTask::getCreateTime));
        return Result.success(taskPage);
    }

    // ==================== 题库 CRUD ====================

    /**
     * 手动创建题目
     * POST /api/question/create
     */
    @PostMapping("/question/create")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> createQuestion(@RequestBody Question question) {
        if (StrUtil.isBlank(question.getContent())) {
            throw new BizException(400, "题目内容不能为空");
        }
        
        // 查重
        String hash = DigestUtil.md5Hex(question.getContent().trim());
        if (questionService.checkDuplicate(question.getContent())) {
            throw new BizException(400, "题库中已存在相同内容的题目");
        }
        
        Long userId = getCurrentUserId();
        question.setContentHash(hash);
        question.setCreateBy(userId);
        question.setUpdateBy(userId);
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());
        
        questionService.save(question);
        return Result.success(question, "题目创建成功");
    }

    /**
     * 更新题目
     * PUT /api/question/update
     */
    @PutMapping("/question/update")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> updateQuestion(@RequestBody Question question) {
        if (question.getId() == null) {
            throw new BizException(400, "ID不能为空");
        }
        
        // 权限校验：如果是教师，只能修改自己创建的，或者自己课程下的(视业务需求，这里严格点：自己创建的)
        // 简单起见：如果是教师，校验是否是题目创建者
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        
        Question exists = questionService.getById(question.getId());
        if (exists == null) throw new BizException(404, "题目不存在");
        
        if (role == 2 && !exists.getCreateBy().equals(userId)) {
             throw new BizException(403, "只能修改自己创建的题目");
        }
        
        // 如果内容变了，重新计算Hash
        if (StrUtil.isNotBlank(question.getContent()) && !question.getContent().equals(exists.getContent())) {
            String hash = DigestUtil.md5Hex(question.getContent().trim());
            // 查重（排除自己）
            long count = questionService.count(new LambdaQueryWrapper<Question>()
                    .eq(Question::getContentHash, hash)
                    .ne(Question::getId, question.getId()));
            if (count > 0) {
                throw new BizException(400, "修改后的内容与现有题目重复");
            }
            question.setContentHash(hash);
        }
        
        question.setUpdateBy(userId);
        question.setUpdateTime(LocalDateTime.now());
        
        questionService.updateById(question);
        return Result.success("题目更新成功");
    }

    /**
     * 批量删除题目
     * DELETE /api/question/batch
     */
    @DeleteMapping("/question/batch")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(400, "ID列表不能为空");
        }
        
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        
        // 如果是教师，只能删除自己创建的
        if (role == 2) {
            long count = questionService.count(new LambdaQueryWrapper<Question>()
                    .in(Question::getId, ids)
                    .ne(Question::getCreateBy, userId));
            if (count > 0) {
                throw new BizException(403, "包含非本人创建的题目，无法删除");
            }
        }
        
        questionService.removeBatchByIds(ids);
        return Result.success("批量删除成功");
    }

    /**
     * 题目列表查询
     * GET /api/question/list
     */
    @GetMapping("/question/list")
    public Result<?> getList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) Integer type) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(content)) {
            query.like(Question::getContent, content);
        }
        if (type != null) {
            query.eq(Question::getType, type);
        }
        if (courseId != null) {
            query.eq(Question::getCourseId, courseId);
        }
        
        // 权限控制逻辑
        if (role == 2) { // 教师
            // 策略：教师只能看到自己教的课程下的题目 OR 自己创建的题目
            // 1. 获取教师教的课程ID列表
            List<Long> courseIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getUserId, userId)
                    .eq(CourseUser::getRole, 2))
                    .stream().map(CourseUser::getCourseId).collect(Collectors.toList());
            
            if (courseIds.isEmpty()) {
                // 如果没课，只能看自己创建的
                query.eq(Question::getCreateBy, userId);
            } else {
                // (course_id IN (...) OR create_by = userId)
                query.and(w -> w.in(Question::getCourseId, courseIds)
                                .or()
                                .eq(Question::getCreateBy, userId));
            }
        }
        // 管理员(3)默认看所有，不做额外限制
        
        query.orderByDesc(Question::getCreateTime);
        return Result.success(questionService.page(new Page<>(page, size), query));
    }

    /**
     * Excel 导入
     * POST /api/question/import
     */
    @PostMapping("/question/import")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> importQuestions(@RequestParam("file") MultipartFile file,
                                     @RequestParam("courseId") Long courseId) {
        Long userId = getCurrentUserId();
        questionService.importQuestions(courseId, file, userId);
        return Result.success("导入处理完成");
    }

    // ==================== 内部回调 ====================

    /**
     * 内部查重接口 (供 Dify 或其他服务调用)
     * POST /api/internal/question/check-duplicate
     */
    @PostMapping("/internal/question/check-duplicate")
    public Result<?> checkDuplicateInternal(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        boolean isDuplicate = questionService.checkDuplicate(content);
        return Result.success(Map.of("is_duplicate", isDuplicate));
    }

    // ==================== DTO & Utils ====================

    @Data
    public static class AiGenerateRequest {
        private Long courseId;
        private String topic;
        private Integer totalCount;
        private String difficulty;
        /**
         * 题目类型: 1-单选, 2-多选, 3-判断, 4-简答, 5-填空 (空则随机)
         */
        private Integer type; 
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Integer getCurrentUserRole() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "").transform(Integer::parseInt);
    }
}
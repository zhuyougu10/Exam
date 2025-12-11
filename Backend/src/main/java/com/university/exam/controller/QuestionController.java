package com.university.exam.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.dto.QuestionExcelDto;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.AiTask;
import com.university.exam.entity.Question;
import com.university.exam.service.AiTaskService;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.QuestionGenerationService;
import com.university.exam.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题库管理控制器
 *
 * @author MySQL数据库架构师
 * @version 1.5.0 (传递Token)
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

    // ==================== 导入导出相关 ====================

    @GetMapping("/question/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("题目导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<QuestionExcelDto> list = new ArrayList<>();
        
        // 示例1：单选题
        QuestionExcelDto q1 = new QuestionExcelDto();
        q1.setContent("Java中int类型占用几个字节？");
        q1.setType(1);
        q1.setDifficulty(1);
        q1.setOptions("2个 | 4个 | 8个 | 16个");
        q1.setAnswer("B");
        q1.setAnalysis("Java中int类型固定占用4个字节。");
        q1.setTags("Java基础, 数据类型");
        list.add(q1);

        // 示例2：多选题
        QuestionExcelDto q2 = new QuestionExcelDto();
        q2.setContent("以下哪些是Java的集合类？");
        q2.setType(2);
        q2.setDifficulty(2);
        q2.setOptions("ArrayList | HashMap | String | HashSet");
        q2.setAnswer("A,B,D");
        q2.setAnalysis("String不是集合类。");
        q2.setTags("Java集合");
        list.add(q2);

        // 示例3：判断题
        QuestionExcelDto q3 = new QuestionExcelDto();
        q3.setContent("Java支持多重继承。");
        q3.setType(3);
        q3.setDifficulty(1);
        q3.setOptions("");
        q3.setAnswer("错误");
        q3.setAnalysis("Java类不支持多重继承，接口支持。");
        list.add(q3);
        
        // 示例4：填空题
        QuestionExcelDto q4 = new QuestionExcelDto();
        q4.setContent("Java的创始人是____，他在____年发布了Java语言。");
        q4.setType(5);
        q4.setDifficulty(1);
        q4.setOptions("");
        q4.setAnswer("James Gosling | 1995");
        q4.setAnalysis("1995年5月23日，Java语言诞生。");
        q4.setTags("历史");
        list.add(q4);

        EasyExcel.write(response.getOutputStream(), QuestionExcelDto.class)
                .sheet("题目导入模板")
                .doWrite(list);
    }

    @PostMapping("/question/import")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> importQuestions(@RequestParam("file") MultipartFile file,
                                     @RequestParam("courseId") Long courseId) {
        Long userId = getCurrentUserId();
        questionService.importQuestions(courseId, file, userId);
        return Result.success("导入处理完成");
    }

    // ==================== AI 生成相关 ====================

    @PostMapping("/question/ai-generate")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> startAiGeneration(@RequestBody AiGenerateRequest req, HttpServletRequest request) {
        Long userId = getCurrentUserId();
        
        // 获取当前请求的 Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long taskId = generationService.startGenerationTask(
                req.getCourseId(), req.getTopic(), req.getTotalCount(),
                req.getDifficulty(), req.getTypes(), userId, token
        );
        return Result.success(Map.of("taskId", taskId), "AI 出题任务已启动");
    }

    @GetMapping("/question/task/list")
    public Result<?> getTaskList(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        IPage<AiTask> taskPage = aiTaskService.page(new Page<>(page, size),
                new LambdaQueryWrapper<AiTask>()
                        .eq(AiTask::getCreateBy, userId)
                        .eq(AiTask::getType, 1)
                        .orderByDesc(AiTask::getCreateTime));
        return Result.success(taskPage);
    }

    @DeleteMapping("/question/task/clear")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> clearFinishedTasks() {
        Long userId = getCurrentUserId();
        aiTaskService.remove(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getCreateBy, userId)
                .in(AiTask::getStatus, 2, 3));
        return Result.success("已清除历史任务");
    }

    // ==================== 题库 CRUD ====================

    @PostMapping("/question/create")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> createQuestion(@RequestBody Question question) {
        if (StrUtil.isBlank(question.getContent())) {
            throw new BizException(400, "题目内容不能为空");
        }
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

    @PutMapping("/question/update")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> updateQuestion(@RequestBody Question question) {
        if (question.getId() == null) throw new BizException(400, "ID不能为空");
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        Question exists = questionService.getById(question.getId());
        if (exists == null) throw new BizException(404, "题目不存在");
        if (role == 2 && !exists.getCreateBy().equals(userId)) {
            throw new BizException(403, "只能修改自己创建的题目");
        }
        if (StrUtil.isNotBlank(question.getContent()) && !question.getContent().equals(exists.getContent())) {
            String hash = DigestUtil.md5Hex(question.getContent().trim());
            long count = questionService.count(new LambdaQueryWrapper<Question>()
                    .eq(Question::getContentHash, hash).ne(Question::getId, question.getId()));
            if (count > 0) throw new BizException(400, "修改后的内容与现有题目重复");
            question.setContentHash(hash);
        }
        question.setUpdateBy(userId);
        question.setUpdateTime(LocalDateTime.now());
        questionService.updateById(question);
        return Result.success("题目更新成功");
    }

    @DeleteMapping("/question/batch")
    @PreAuthorize("hasAnyRole(2, 3)")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException(400, "ID列表不能为空");
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        if (role == 2) {
            long count = questionService.count(new LambdaQueryWrapper<Question>()
                    .in(Question::getId, ids).ne(Question::getCreateBy, userId));
            if (count > 0) throw new BizException(403, "包含非本人创建的题目，无法删除");
        }
        questionService.removeBatchByIds(ids);
        return Result.success("批量删除成功");
    }

    @GetMapping("/question/list")
    public Result<?> getList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) Integer type) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(content)) query.like(Question::getContent, content);
        if (type != null) query.eq(Question::getType, type);
        if (courseId != null) query.eq(Question::getCourseId, courseId);
        if (role == 2) {
            List<Long> courseIds = courseUserService.list(new LambdaQueryWrapper<com.university.exam.entity.CourseUser>()
                            .eq(com.university.exam.entity.CourseUser::getUserId, userId)
                            .eq(com.university.exam.entity.CourseUser::getRole, 2))
                    .stream().map(com.university.exam.entity.CourseUser::getCourseId).collect(Collectors.toList());
            if (courseIds.isEmpty()) query.eq(Question::getCreateBy, userId);
            else query.and(w -> w.in(Question::getCourseId, courseIds).or().eq(Question::getCreateBy, userId));
        }
        query.orderByDesc(Question::getCreateTime);
        return Result.success(questionService.page(new Page<>(page, size), query));
    }

    @PostMapping("/internal/question/check-duplicate")
    public Result<?> checkDuplicateInternal(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        boolean isDuplicate = questionService.checkDuplicate(content);
        return Result.success(Map.of("is_duplicate", isDuplicate));
    }

    @Data
    public static class AiGenerateRequest {
        private Long courseId;
        private String topic;
        private Integer totalCount;
        private String difficulty;
        private List<Integer> types;
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Integer getCurrentUserRole() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "").transform(Integer::parseInt);
    }
}
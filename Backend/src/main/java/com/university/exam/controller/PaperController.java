package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.Paper;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.PaperService;
import com.university.exam.service.PublishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 试卷管理控制器
 *
 * @author MySQL数据库架构师
 * @version 1.1.0 (增加数据强校验)
 * @since 2025-12-11
 */
@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;
    private final PublishService publishService;
    private final CourseUserService courseUserService;

    /**
     * 智能/随机组卷
     */
    @PostMapping("/random-create")
    @PreAuthorize("hasAnyRole('2', '3')")
    public Result<?> randomCreate(@Valid @RequestBody PaperService.RandomPaperRequest request) {
        Long userId = getCurrentUserId();
        Long paperId = paperService.randomCreate(request, userId);
        return Result.success(Map.of("id", paperId), "智能组卷成功");
    }

    /**
     * 手动组卷
     */
    @PostMapping("/manual-create")
    @PreAuthorize("hasAnyRole('2', '3')")
    public Result<?> manualCreate(@Valid @RequestBody PaperService.ManualPaperRequest request) {
        Long userId = getCurrentUserId();
        Long paperId = paperService.manualCreate(request, userId);
        return Result.success(Map.of("id", paperId), "手动组卷成功");
    }

    /**
     * 分页查询试卷列表
     */
    @GetMapping("/list")
    public Result<?> getList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String title) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        LambdaQueryWrapper<Paper> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            query.like(Paper::getTitle, title);
        }
        if (courseId != null) {
            query.eq(Paper::getCourseId, courseId);
        }

        // 权限控制
        if (role == 2) { // 教师：查看所教课程的试卷 或 自己创建的
            List<Long> courseIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                            .eq(CourseUser::getUserId, userId)
                            .eq(CourseUser::getRole, 2))
                    .stream().map(CourseUser::getCourseId).collect(Collectors.toList());

            if (courseIds.isEmpty()) {
                query.eq(Paper::getCreateBy, userId);
            } else {
                query.and(w -> w.in(Paper::getCourseId, courseIds)
                        .or().eq(Paper::getCreateBy, userId));
            }
        }
        // 管理员看所有 (role == 3)

        query.orderByDesc(Paper::getCreateTime);
        IPage<Paper> result = paperService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('2', '3')")
    public Result<?> delete(@PathVariable Long id) {
        // 检查是否已发布
        boolean isPublished = publishService.checkIsPublished(id);
        if (isPublished) {
            throw new BizException(400, "该试卷已发布过考试，无法删除");
        }

        Long userId = getCurrentUserId();
        Paper paper = paperService.getById(id);
        if (paper == null) throw new BizException(404, "试卷不存在");

        // 权限检查
        if (getCurrentUserRole() == 2 && !paper.getCreateBy().equals(userId)) {
            throw new BizException(403, "无权删除非本人创建的试卷");
        }

        // 逻辑删除
        paper.setUpdateBy(userId);
        paper.setUpdateTime(LocalDateTime.now());
        paperService.removeById(id);

        return Result.success("删除成功");
    }

    /**
     * 获取试卷详情 (用于预览)
     */
    @GetMapping("/{id}")
    public Result<?> getDetail(@PathVariable Long id) {
        Paper paper = paperService.getById(id);
        if (paper == null) throw new BizException(404, "试卷不存在");
        return Result.success(paper);
    }

    /**
     * 获取试卷详情（含题目列表，用于预览和编辑）
     */
    @GetMapping("/{id}/detail")
    public Result<?> getPaperWithQuestions(@PathVariable Long id) {
        return Result.success(paperService.getPaperDetail(id));
    }

    /**
     * 添加题目到试卷
     */
    @PostMapping("/{id}/add-question")
    @PreAuthorize("hasAnyRole('2', '3')")
    public Result<?> addQuestionToPaper(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long questionId = Long.valueOf(body.get("questionId").toString());
        java.math.BigDecimal score = new java.math.BigDecimal(body.get("score").toString());
        paperService.addQuestionToPaper(id, questionId, score, getCurrentUserId());
        return Result.success("添加成功");
    }

    /**
     * 从试卷移除题目
     */
    @DeleteMapping("/{paperId}/question/{questionId}")
    @PreAuthorize("hasAnyRole('2', '3')")
    public Result<?> removeQuestionFromPaper(@PathVariable Long paperId, @PathVariable Long questionId) {
        paperService.removeQuestionFromPaper(paperId, questionId, getCurrentUserId());
        return Result.success("移除成功");
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Integer getCurrentUserRole() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "").transform(Integer::parseInt);
    }
}
package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.Paper;
import com.university.exam.entity.Publish;
import com.university.exam.entity.Record;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.PaperService;
import com.university.exam.service.PublishService;
import com.university.exam.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试发布管理控制器
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-11
 */
@RestController
@RequestMapping("/api/exam/publish")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('2', '3')")
public class ExamPublishController {

    private final PublishService publishService;
    private final PaperService paperService;
    private final CourseUserService courseUserService;
    private final RecordService recordService;

    /**
     * 发布考试
     * POST /api/exam/publish
     */
    @PostMapping
    public Result<?> publish(@RequestBody PublishService.PublishRequest request) {
        Long userId = getCurrentUserId();
        
        // 校验试卷权限
        Paper paper = paperService.getById(request.getPaperId());
        if (paper == null) throw new BizException(404, "试卷不存在");
        
        // 教师只能发布自己课程的试卷
        if (getCurrentUserRole() == 2) {
            // 校验是否有该课程权限
            long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getUserId, userId)
                    .eq(CourseUser::getCourseId, paper.getCourseId())
                    .eq(CourseUser::getRole, 2));
            if (count == 0 && !paper.getCreateBy().equals(userId)) {
                throw new BizException(403, "无权发布该试卷");
            }
        }

        Long publishId = publishService.publishExam(request, userId);
        return Result.success(Map.of("id", publishId), "考试发布成功，已通知相关学生");
    }

    /**
     * 获取发布列表（含统计数据）
     * GET /api/exam/publish/list
     */
    @GetMapping("/list")
    public Result<?> getList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Long paperId,
                             @RequestParam(required = false) Integer status) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        LambdaQueryWrapper<Publish> query = new LambdaQueryWrapper<>();
        if (paperId != null) {
            query.eq(Publish::getPaperId, paperId);
        }
        if (status != null) {
            query.eq(Publish::getStatus, status);
        }

        // 教师只能看自己发布的
        if (role == 2) {
            query.eq(Publish::getCreateBy, userId);
        }

        query.orderByDesc(Publish::getCreateTime);
        IPage<Publish> result = publishService.page(new Page<>(page, size), query);
        
        // 获取所有publishId
        List<Long> publishIds = result.getRecords().stream().map(Publish::getId).toList();
        if (publishIds.isEmpty()) {
            return Result.success(result);
        }
        
        // 查询所有相关记录
        List<Record> records = recordService.list(new LambdaQueryWrapper<Record>()
                .in(Record::getPublishId, publishIds));
        
        // 按publishId分组统计
        Map<Long, List<Record>> recordMap = records.stream()
                .collect(Collectors.groupingBy(Record::getPublishId));
        
        // 获取试卷标题
        List<Long> paperIds = result.getRecords().stream().map(Publish::getPaperId).distinct().toList();
        Map<Long, String> paperTitleMap = paperService.listByIds(paperIds).stream()
                .collect(Collectors.toMap(Paper::getId, Paper::getTitle));
        
        // 组装返回数据
        List<Map<String, Object>> voList = result.getRecords().stream().map(p -> {
            List<Record> pRecords = recordMap.getOrDefault(p.getId(), List.of());
            long totalCount = pRecords.size();
            long inProgressCount = pRecords.stream().filter(r -> r.getStatus() == 1).count();
            long submittedCount = pRecords.stream().filter(r -> r.getStatus() >= 2).count();
            
            Map<String, Object> vo = new java.util.HashMap<>();
            vo.put("id", p.getId());
            vo.put("title", p.getTitle());
            vo.put("paperId", p.getPaperId());
            vo.put("paperTitle", paperTitleMap.getOrDefault(p.getPaperId(), ""));
            vo.put("startTime", p.getStartTime());
            vo.put("endTime", p.getEndTime());
            vo.put("status", p.getStatus());
            vo.put("totalCount", totalCount);
            vo.put("inProgressCount", inProgressCount);
            vo.put("submittedCount", submittedCount);
            return vo;
        }).toList();
        
        Map<String, Object> pageResult = new java.util.HashMap<>();
        pageResult.put("records", voList);
        pageResult.put("total", result.getTotal());
        pageResult.put("current", result.getCurrent());
        pageResult.put("size", result.getSize());
        
        return Result.success(pageResult);
    }

    /**
     * 删除/撤销发布
     * DELETE /api/exam/publish/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Publish publish = publishService.getById(id);
        if (publish == null) throw new BizException(404, "发布记录不存在");

        Long userId = getCurrentUserId();
        if (getCurrentUserRole() == 2 && !publish.getCreateBy().equals(userId)) {
            throw new BizException(403, "无权操作");
        }

        // 检查是否有考试记录
        long recordCount = recordService.count(new LambdaQueryWrapper<Record>()
                .eq(Record::getPublishId, id));
        if (recordCount > 0) {
            throw new BizException(400, "该考试已有学生参加，无法删除");
        }

        // 逻辑删除
        publish.setUpdateBy(userId);
        publish.setUpdateTime(LocalDateTime.now());
        publishService.removeById(id);

        return Result.success("撤销成功");
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Integer getCurrentUserRole() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "").transform(Integer::parseInt);
    }
}
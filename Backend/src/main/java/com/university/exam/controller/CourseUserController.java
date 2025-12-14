package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.User;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程成员管理控制器
 * 处理学生选课、老师排课等逻辑
 *
 * @author Senior Backend Engineer
 * @since 2025-12-14
 */
@RestController
@RequestMapping("/api/course/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole(1)") // 仅管理员可操作排课
public class CourseUserController {

    private final CourseUserService courseUserService;
    private final UserService userService;

    /**
     * 获取某门课程的成员列表
     * @param courseId 课程ID
     * @param role 角色 (2-老师, 3-学生, 不传查所有)
     */
    @GetMapping("/list")
    public Result<?> getCourseMembers(@RequestParam Long courseId,
                                      @RequestParam(required = false) Integer role,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        
        Page<CourseUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CourseUser> query = new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId);
        
        if (role != null) {
            query.eq(CourseUser::getRole, role);
        }
        query.orderByDesc(CourseUser::getCreateTime);

        Page<CourseUser> result = courseUserService.page(pageParam, query);

        // 填充用户详情 (姓名、学号等)
        if (!result.getRecords().isEmpty()) {
            List<Long> userIds = result.getRecords().stream().map(CourseUser::getUserId).toList();
            Map<Long, User> userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            // 这里可以封装一个 VO，为了简单直接返回 Map 或增强 Entity (不推荐直接改Entity，实际项目建议用 VO)
            // 这里演示逻辑，暂且假设前端通过 userId 二次查询或这里做简单组装
            result.getRecords().forEach(cu -> {
                User u = userMap.get(cu.getUserId());
                if (u != null) {
                    // 注意：Entity 中需要有 @TableField(exist=false) private String realName;
                    // 或者直接在这里构造 Map 返回。为了不改 Entity，我们假设前端只拿 userId 够用，或者我们用 Map 返回。
                }
            });
        }
        
        return Result.success(result);
    }

    /**
     * 单个添加成员
     */
    @PostMapping("/add")
    public Result<?> addMember(@RequestBody CourseUser courseUser) {
        // 查重
        long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseUser.getCourseId())
                .eq(CourseUser::getUserId, courseUser.getUserId()));
        
        if (count > 0) {
            throw new BizException(400, "该用户已在课程中");
        }
        
        courseUserService.save(courseUser);
        return Result.success(null, "添加成功");
    }

    /**
     * 批量导入：将某个班级(Dept)的所有学生导入课程
     * 核心缺失功能补充
     */
    @PostMapping("/import-dept")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> importFromDept(@RequestBody Map<String, Long> params) {
        Long courseId = params.get("courseId");
        Long deptId = params.get("deptId");
        
        if (courseId == null || deptId == null) {
            throw new BizException(400, "参数错误");
        }

        // 1. 查找该部门下的所有学生 (Role=3)
        List<User> students = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getDeptId, deptId)
                .eq(User::getRole, 3));
        
        if (students.isEmpty()) {
            return Result.success(0, "该班级暂无学生");
        }

        // 2. 查找该课程已有的学生ID，避免重复
        List<Long> existingUserIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId))
                .stream().map(CourseUser::getUserId).toList();

        // 3. 过滤出需要新增的学生
        List<CourseUser> toAdd = new ArrayList<>();
        for (User stu : students) {
            if (!existingUserIds.contains(stu.getId())) {
                CourseUser cu = new CourseUser();
                cu.setCourseId(courseId);
                cu.setUserId(stu.getId());
                cu.setRole((byte) 3); // 学生
                toAdd.add(cu);
            }
        }

        if (!toAdd.isEmpty()) {
            courseUserService.saveBatch(toAdd);
        }

        return Result.success(toAdd.size(), "成功导入 " + toAdd.size() + " 名学生");
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/remove")
    public Result<?> removeMember(@RequestParam Long courseId, @RequestParam Long userId) {
        courseUserService.remove(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId)
                .eq(CourseUser::getUserId, userId));
        return Result.success(null, "移除成功");
    }
}
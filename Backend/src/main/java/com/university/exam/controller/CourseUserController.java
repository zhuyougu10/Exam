package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.Course;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.Dept;
import com.university.exam.entity.User;
import com.university.exam.service.CourseService;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.DeptService;
import com.university.exam.service.UserService;
import com.university.exam.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程成员管理控制器
 * 处理学生选课、老师排课等逻辑
 * sys_course_user.role: 1-学生, 2-教师
 * sys_user.role: 1-学生, 2-教师, 3-管理员
 *
 * @author Senior Backend Engineer
 * @since 2025-12-14
 */
@RestController
@RequestMapping("/api/course/user")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('2', '3')") // 教师和管理员
public class CourseUserController {

    private final CourseUserService courseUserService;
    private final UserService userService;
    private final DeptService deptService;
    private final CourseService courseService;
    private final JwtUtils jwtUtils;

    /**
     * 搜索用户（按学号或姓名）
     * 用于添加学生到课程
     */
    @GetMapping("/search-users")
    public Result<?> searchUsers(@RequestParam String keyword,
                                 @RequestParam(required = false) Integer role) {
        if (keyword == null || keyword.length() < 2) {
            return Result.success(new ArrayList<>());
        }
        
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .and(q -> q.like(User::getUsername, keyword)
                        .or().like(User::getRealName, keyword));
        
        if (role != null) {
            query.eq(User::getRole, role);
        }
        
        query.last("LIMIT 20");
        
        List<User> users = userService.list(query);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 获取当前用户已加入的课程列表
     * 用于教师端知识库、题库等页面筛选
     */
    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('2', '3')") // 教师和管理员
    public Result<?> getMyCourses(HttpServletRequest request) {
        Long userId = jwtUtils.getUserIdFromToken(getToken(request));
        
        // 查询用户加入的课程ID列表
        List<CourseUser> courseUsers = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getUserId, userId));
        
        if (courseUsers.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        List<Long> courseIds = courseUsers.stream().map(CourseUser::getCourseId).distinct().toList();
        
        // 查询课程详情
        List<Course> courses = courseService.listByIds(courseIds);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", course.getId());
            item.put("courseName", course.getCourseName());
            item.put("courseCode", course.getCourseCode());
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 获取课程关联的班级列表（已有学生加入的班级）
     * 用于考试发布时筛选可选班级
     */
    @GetMapping("/depts")
    @PreAuthorize("hasAnyRole('2', '3')") // 教师和管理员都可访问
    public Result<?> getCourseDepts(@RequestParam Long courseId) {
        // 1. 获取课程中所有学生的ID
        List<CourseUser> courseUsers = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId)
                .eq(CourseUser::getRole, 1)); // 只查学生
        
        if (courseUsers.isEmpty()) {
            return Result.success(new ArrayList<>(), "该课程暂无学生");
        }
        
        // 2. 获取这些学生所属的部门ID
        List<Long> userIds = courseUsers.stream().map(CourseUser::getUserId).toList();
        List<User> students = userService.listByIds(userIds);
        List<Long> deptIds = students.stream()
                .map(User::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        
        if (deptIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 3. 获取部门信息并构建树形结构
        List<Dept> depts = deptService.listByIds(deptIds);
        
        // 返回扁平列表，前端可直接使用
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dept dept : depts) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", dept.getId());
            item.put("deptName", dept.getDeptName());
            item.put("category", dept.getCategory());
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 获取课程的所有学期列表
     */
    @GetMapping("/semesters")
    public Result<?> getCourseSemesters(@RequestParam Long courseId) {
        List<String> semesters = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId)
                .select(CourseUser::getSemester))
                .stream()
                .map(CourseUser::getSemester)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted((a, b) -> b.compareTo(a)) // 降序排列，最新学期在前
                .toList();
        
        // 确保当前学期在列表中
        String currentSemester = getCurrentSemester();
        if (!semesters.contains(currentSemester)) {
            List<String> result = new ArrayList<>();
            result.add(currentSemester);
            result.addAll(semesters);
            return Result.success(result);
        }
        return Result.success(semesters);
    }

    /**
     * 获取某门课程的成员列表
     * @param courseId 课程ID
     * @param role 角色 (1-学生, 2-教师, 不传查所有)
     * @param semester 学期筛选
     */
    @GetMapping("/list")
    public Result<?> getCourseMembers(@RequestParam Long courseId,
                                      @RequestParam(required = false) Integer role,
                                      @RequestParam(required = false) String semester,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "100") Integer size) {
        
        Page<CourseUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CourseUser> query = new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId);
        
        if (role != null) {
            query.eq(CourseUser::getRole, role);
        }
        if (semester != null && !semester.isEmpty()) {
            query.eq(CourseUser::getSemester, semester);
        }
        query.orderByDesc(CourseUser::getCreateTime);

        Page<CourseUser> result = courseUserService.page(pageParam, query);

        // 填充用户详情和部门信息
        List<Map<String, Object>> records = new ArrayList<>();
        if (!result.getRecords().isEmpty()) {
            List<Long> userIds = result.getRecords().stream().map(CourseUser::getUserId).toList();
            List<User> users = userService.listByIds(userIds);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            
            // 获取所有部门信息
            List<Long> deptIds = users.stream()
                    .map(User::getDeptId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            Map<Long, Dept> deptMap = new HashMap<>();
            if (!deptIds.isEmpty()) {
                deptMap = deptService.listByIds(deptIds).stream()
                        .collect(Collectors.toMap(Dept::getId, d -> d));
            }

            for (CourseUser cu : result.getRecords()) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", cu.getId());
                record.put("courseId", cu.getCourseId());
                record.put("userId", cu.getUserId());
                record.put("role", cu.getRole());
                record.put("semester", cu.getSemester());
                record.put("createTime", cu.getCreateTime());
                
                User u = userMap.get(cu.getUserId());
                if (u != null) {
                    record.put("username", u.getUsername());
                    record.put("realName", u.getRealName());
                    record.put("avatar", u.getAvatar());
                    record.put("deptId", u.getDeptId());
                    
                    Dept dept = deptMap.get(u.getDeptId());
                    record.put("deptName", dept != null ? dept.getDeptName() : "未分配班级");
                }
                records.add(record);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("total", result.getTotal());
        
        return Result.success(response);
    }

    /**
     * 单个添加成员
     */
    @PostMapping("/add")
    public Result<?> addMember(@RequestBody CourseUser courseUser) {
        // 设置默认学期
        if (courseUser.getSemester() == null || courseUser.getSemester().isEmpty()) {
            courseUser.setSemester(getCurrentSemester());
        }
        
        // 查重（唯一约束：course_id + user_id + semester）
        long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseUser.getCourseId())
                .eq(CourseUser::getUserId, courseUser.getUserId())
                .eq(CourseUser::getSemester, courseUser.getSemester()));
        
        if (count > 0) {
            throw new BizException(400, "该用户在本学期已在课程中");
        }
        
        courseUserService.save(courseUser);
        return Result.success(null, "添加成功");
    }

    /**
     * 批量导入：将某个部门及其所有子部门下的学生导入课程
     * 支持选择学院/系级别，自动导入下属所有班级的学生
     */
    @PostMapping("/import-dept")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> importFromDept(@RequestBody Map<String, Object> params) {
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : null;
        Long deptId = params.get("deptId") != null ? Long.valueOf(params.get("deptId").toString()) : null;
        String semester = params.get("semester") != null ? params.get("semester").toString() : null;
        
        if (courseId == null || deptId == null) {
            throw new BizException(400, "参数错误");
        }
        
        // 如果没有指定学期，使用当前学期
        if (semester == null || semester.isEmpty()) {
            semester = getCurrentSemester();
        }

        // 1. 获取该部门及其所有子部门的ID列表
        List<Long> allDeptIds = new ArrayList<>();
        allDeptIds.add(deptId);
        collectChildDeptIds(deptId, allDeptIds);

        // 2. 查找这些部门下的所有学生 (sys_user.role=1 表示学生)
        List<User> students = userService.list(new LambdaQueryWrapper<User>()
                .in(User::getDeptId, allDeptIds)
                .eq(User::getRole, 1));
        
        if (students.isEmpty()) {
            return Result.success(0, "该部门及其子部门下暂无学生");
        }

        // 3. 查找该课程该学期已有的学生ID，避免重复（唯一约束：course_id + user_id + semester）
        List<Long> existingUserIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getCourseId, courseId)
                .eq(CourseUser::getSemester, semester))
                .stream().map(CourseUser::getUserId).toList();

        // 4. 过滤出需要新增的学生
        final String finalSemester = semester;
        List<CourseUser> toAdd = new ArrayList<>();
        for (User stu : students) {
            if (!existingUserIds.contains(stu.getId())) {
                CourseUser cu = new CourseUser();
                cu.setCourseId(courseId);
                cu.setUserId(stu.getId());
                cu.setRole((byte) 1); // sys_course_user.role=1 表示学生
                cu.setSemester(finalSemester);
                toAdd.add(cu);
            }
        }

        if (!toAdd.isEmpty()) {
            courseUserService.saveBatch(toAdd);
        }

        return Result.success(toAdd.size(), "成功导入 " + toAdd.size() + " 名学生");
    }

    /**
     * 递归收集所有子部门ID
     */
    private void collectChildDeptIds(Long parentId, List<Long> result) {
        List<Dept> children = deptService.list(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getParentId, parentId));
        for (Dept child : children) {
            result.add(child.getId());
            collectChildDeptIds(child.getId(), result);
        }
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

    /**
     * 获取当前学期（格式如：2024-2025-1）
     */
    private String getCurrentSemester() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        // 9月-次年1月为第一学期，2月-8月为第二学期
        if (month >= 9) {
            return year + "-" + (year + 1) + "-1";
        } else if (month <= 1) {
            return (year - 1) + "-" + year + "-1";
        } else {
            return (year - 1) + "-" + year + "-2";
        }
    }

    /**
     * 从请求头中获取Token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }
}
package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.Config;
import com.university.exam.entity.Course;
import com.university.exam.entity.Dept;
import com.university.exam.entity.User;
import com.university.exam.entity.CourseUser;
import com.university.exam.service.ConfigService;
import com.university.exam.service.CourseService;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.DeptService;
import com.university.exam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器
 * 处理用户管理、组织架构、课程管理和系统设置相关请求
 *
 * @author MySQL数据库架构师
 * @version 1.1.0
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('3')")
public class SysAdminController {

    private final UserService userService;
    private final DeptService deptService;
    private final CourseService courseService;
    private final CourseUserService courseUserService;
    private final ConfigService configService;
    private final BCryptPasswordEncoder passwordEncoder;

    // ===================== 用户管理 =====================

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建结果
     */
    @PostMapping("/user")
    public Result<?> createUser(@Valid @RequestBody User user) {
        // 1. 参数验证
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BizException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BizException(400, "密码不能为空");
        }

        // 2. 检查用户名是否已存在
        User existingUser = userService.lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .one();
        if (existingUser != null) {
            throw new BizException(400, "用户名已存在");
        }

        // 3. 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 4. 设置创建人和创建时间
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        user.setCreateBy(currentUserId);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateBy(currentUserId);
        user.setUpdateTime(LocalDateTime.now());

        // 5. 数据持久化
        userService.save(user);

        return Result.success("用户创建成功");
    }

    /**
     * 更新用户信息
     *
     * @param id   用户ID
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/user/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        // 1. 检查用户是否存在
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            throw new BizException(404, "用户不存在");
        }

        // 2. 如果更新了用户名，检查是否已存在
        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(existingUser.getUsername())) {
            User checkUser = userService.lambdaQuery()
                    .eq(User::getUsername, user.getUsername())
                    .ne(User::getId, id)
                    .one();
            if (checkUser != null) {
                throw new BizException(400, "用户名已存在");
            }
        }

        // 3. 仅更新非空字段
        user.setId(id);
        if (!StringUtils.hasText(user.getPassword())) {
            // 密码为空则不更新
            user.setPassword(null);
        } else {
            // 密码不为空则加密更新
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 4. 设置更新人和更新时间
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        user.setUpdateBy(currentUserId);
        user.setUpdateTime(LocalDateTime.now());

        // 5. 数据更新
        userService.updateById(user);

        return Result.success("用户更新成功");
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/user/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        // 1. 检查用户是否存在
        User user = userService.getById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 2. 处理关联数据（这里简化处理，实际项目中可能需要处理更多关联）
        // TODO: 处理用户相关的关联数据，如考试记录、错题本等

        // 3. 删除用户
        userService.removeById(id);

        return Result.success("用户删除成功");
    }

    /**
     * 获取用户列表
     *
     * @param page     页码
     * @param size     每页条数
     * @param username 用户名搜索
     * @param deptId   部门ID
     * @return 用户列表
     */
    @GetMapping("/user/list")
    public Result<?> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer role) {
        // 1. 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            // 支持按用户名或真实姓名模糊搜索
            queryWrapper.and(w -> w
                .like(User::getUsername, username)
                .or()
                .like(User::getRealName, username)
            );
        }
        if (deptId != null) {
            queryWrapper.eq(User::getDeptId, deptId);
        }
        if (role != null) {
            queryWrapper.eq(User::getRole, role);
        }

        // 2. 分页查询
        IPage<User> userPage = userService.page(new Page<>(page, size), queryWrapper);

        // 3. 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("records", userPage.getRecords());
        response.put("total", userPage.getTotal());
        response.put("current", userPage.getCurrent());
        response.put("size", userPage.getSize());

        return Result.success(response, "获取用户列表成功");
    }

    // ===================== 组织架构 =====================

    /**
     * 获取部门组织架构树
     * 前端调用路径: /api/admin/dept/tree
     *
     * @return 部门树形结构数据
     */
    @GetMapping("/dept/tree")
    @PreAuthorize("hasAnyRole('2', '3')")
    public Result<List<Dept>> getDeptTree() {
        // 调用 DeptService 中已实现的递归构建树逻辑
        List<Dept> tree = deptService.getDeptTree();
        return Result.success(tree);
    }


    /**
     * 创建部门
     *
     * @param dept 部门信息
     * @return 创建结果
     */
    @PostMapping("/dept")
    public Result<?> createDept(@Valid @RequestBody Dept dept) {
        // 1. 参数验证
        if (!StringUtils.hasText(dept.getDeptName())) {
            throw new BizException(400, "部门名称不能为空");
        }

        // 2. 设置创建人和创建时间
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        dept.setCreateBy(currentUserId);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateBy(currentUserId);
        dept.setUpdateTime(LocalDateTime.now());

        // 3. 数据持久化
        deptService.save(dept);

        return Result.success("部门创建成功");
    }

    /**
     * 更新部门信息
     *
     * @param id   部门ID
     * @param dept 部门信息
     * @return 更新结果
     */
    @PutMapping("/dept/{id}")
    public Result<?> updateDept(@PathVariable Long id, @RequestBody Dept dept) {
        // 1. 检查部门是否存在
        Dept existingDept = deptService.getById(id);
        if (existingDept == null) {
            throw new BizException(404, "部门不存在");
        }

        // 2. 设置更新人和更新时间
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        dept.setId(id);
        dept.setUpdateBy(currentUserId);
        dept.setUpdateTime(LocalDateTime.now());

        // 3. 数据更新
        deptService.updateById(dept);

        return Result.success("部门更新成功");
    }

    /**
     * 删除部门
     *
     * @param id 部门ID
     * @return 删除结果
     */
    @DeleteMapping("/dept/{id}")
    public Result<?> deleteDept(@PathVariable Long id) {
        // 1. 检查部门是否存在
        Dept dept = deptService.getById(id);
        if (dept == null) {
            throw new BizException(404, "部门不存在");
        }

        // 2. 检查是否存在子部门
        long childCount = deptService.lambdaQuery()
                .eq(Dept::getParentId, id)
                .count();
        if (childCount > 0) {
            throw new BizException(400, "该部门存在子部门，无法删除");
        }

        // 3. 检查是否存在关联用户
        long userCount = userService.lambdaQuery()
                .eq(User::getDeptId, id)
                .count();
        if (userCount > 0) {
            throw new BizException(400, "该部门存在关联用户，无法删除");
        }

        // 4. 删除部门
        deptService.removeById(id);

        return Result.success("部门删除成功");
    }

    // ===================== 课程管理 =====================

    /**
     * 获取课程列表
     *
     * @param page       页码
     * @param size       每页条数
     * @param courseName 课程名称搜索
     * @param deptId     部门ID
     * @return 课程列表
     */
    @GetMapping("/course/list")
    public Result<?> getCourseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Long deptId) {
        // 1. 构建查询条件
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(courseName)) {
            queryWrapper.and(w -> w
                .like(Course::getCourseName, courseName)
                .or()
                .like(Course::getCourseCode, courseName)
            );
        }
        if (deptId != null) {
            queryWrapper.eq(Course::getDeptId, deptId);
        }
        queryWrapper.orderByDesc(Course::getCreateTime);

        // 2. 分页查询
        IPage<Course> coursePage = courseService.page(new Page<>(page, size), queryWrapper);

        // 3. 统计每个课程的教师数和学生数
        List<Map<String, Object>> records = new ArrayList<>();
        for (Course course : coursePage.getRecords()) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", course.getId());
            courseMap.put("courseName", course.getCourseName());
            courseMap.put("courseCode", course.getCourseCode());
            courseMap.put("deptId", course.getDeptId());
            courseMap.put("credits", course.getCredits());
            courseMap.put("coverImg", course.getCoverImg());
            courseMap.put("description", course.getDescription());
            courseMap.put("status", course.getStatus());
            courseMap.put("createTime", course.getCreateTime());
            
            // 统计教师数 (role=2)
            long teacherCount = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getCourseId, course.getId())
                    .eq(CourseUser::getRole, (byte) 2));
            courseMap.put("teacherCount", teacherCount);
            
            // 统计学生数 (role=1)
            long studentCount = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getCourseId, course.getId())
                    .eq(CourseUser::getRole, (byte) 1));
            courseMap.put("studentCount", studentCount);
            
            records.add(courseMap);
        }

        // 4. 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("total", coursePage.getTotal());
        response.put("current", coursePage.getCurrent());
        response.put("size", coursePage.getSize());

        return Result.success(response, "获取课程列表成功");
    }

    /**
     * 创建课程
     *
     * @param course 课程信息
     * @return 创建结果
     */
    @PostMapping("/course")
    public Result<?> createCourse(@Valid @RequestBody Course course) {
        // 1. 参数验证
        if (!StringUtils.hasText(course.getCourseName())) {
            throw new BizException(400, "课程名称不能为空");
        }
        if (!StringUtils.hasText(course.getCourseCode())) {
            throw new BizException(400, "课程代码不能为空");
        }

        // 2. 检查课程代码是否已存在
        Course existingCourse = courseService.lambdaQuery()
                .eq(Course::getCourseCode, course.getCourseCode())
                .one();
        if (existingCourse != null) {
            throw new BizException(400, "课程代码已存在");
        }

        // 3. 设置创建人和创建时间
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        course.setCreateBy(currentUserId);
        course.setCreateTime(LocalDateTime.now());
        course.setUpdateBy(currentUserId);
        course.setUpdateTime(LocalDateTime.now());

        // 4. 数据持久化
        courseService.save(course);

        return Result.success("课程创建成功");
    }

    /**
     * 更新课程
     *
     * @param id     课程ID
     * @param course 课程信息
     * @return 更新结果
     */
    @PutMapping("/course/{id}")
    public Result<?> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        // 1. 检查课程是否存在
        Course existingCourse = courseService.getById(id);
        if (existingCourse == null) {
            throw new BizException(404, "课程不存在");
        }

        // 2. 如果修改了课程代码，检查是否冲突
        if (StringUtils.hasText(course.getCourseCode()) && !course.getCourseCode().equals(existingCourse.getCourseCode())) {
            Course checkCourse = courseService.lambdaQuery()
                    .eq(Course::getCourseCode, course.getCourseCode())
                    .ne(Course::getId, id)
                    .one();
            if (checkCourse != null) {
                throw new BizException(400, "课程代码已存在");
            }
        }

        // 3. 设置更新信息
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        course.setId(id);
        course.setUpdateBy(currentUserId);
        course.setUpdateTime(LocalDateTime.now());

        // 4. 更新
        courseService.updateById(course);

        return Result.success("课程更新成功");
    }

    /**
     * 删除课程
     *
     * @param id 课程ID
     * @return 删除结果
     */
    @DeleteMapping("/course/{id}")
    public Result<?> deleteCourse(@PathVariable Long id) {
        // 1. 检查课程是否存在
        Course course = courseService.getById(id);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }

        // 2. (可选) 检查是否有学生选课，如有则禁止删除
        // long studentCount = courseUserService.lambdaQuery().eq(CourseUser::getCourseId, id).count();
        // if (studentCount > 0) {
        //     throw new BizException(400, "该课程已有学生选修，无法删除");
        // }

        // 3. 删除
        courseService.removeById(id);

        return Result.success("课程删除成功");
    }

    // ===================== 系统设置 =====================

    /**
     * 获取系统配置信息
     * 确保敏感配置项（如密钥、凭证）已脱敏处理
     *
     * @return 系统配置信息
     */
    @GetMapping("/config")
    public Result<?> getSystemConfig() {
        // 1. 获取所有配置
        List<Config> configList = configService.list();

        // 2. 脱敏处理敏感配置项
        List<Config> desensitizedConfigs = configList.stream().map(config -> {
            Config desensitizedConfig = new Config();
            desensitizedConfig.setId(config.getId());
            desensitizedConfig.setConfigKey(config.getConfigKey());
            desensitizedConfig.setDescription(config.getDescription());
            desensitizedConfig.setCreateTime(config.getCreateTime());
            desensitizedConfig.setUpdateTime(config.getUpdateTime());

            // 敏感配置项脱敏处理
            String configKey = config.getConfigKey();
            String configValue = config.getConfigValue();
            if (configKey.toLowerCase().contains("password") || configKey.toLowerCase().contains("key") ||
                    configKey.toLowerCase().contains("secret") || configKey.toLowerCase().contains("token")) {
                // 脱敏处理：只保留前3位和后3位，中间用*代替
                if (configValue != null && configValue.length() > 6) {
                    configValue = configValue.substring(0, 3) + "******" + configValue.substring(configValue.length() - 3);
                } else {
                    configValue = "******";
                }
            }
            desensitizedConfig.setConfigValue(configValue);

            return desensitizedConfig;
        }).toList();

        return Result.success(desensitizedConfigs, "获取系统配置成功");
    }

    /**
     * 更新系统配置
     *
     * @param configList 配置列表
     * @return 更新结果
     */
    @PutMapping("/config")
    public Result<?> updateSystemConfig(@RequestBody List<Config> configList) {
        // 1. 参数验证
        if (configList == null || configList.isEmpty()) {
            throw new BizException(400, "配置列表不能为空");
        }

        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. 更新配置
        for (Config config : configList) {
            if (config.getId() == null || !StringUtils.hasText(config.getConfigKey())) {
                continue;
            }

            // 3. 检查配置是否存在
            Config existingConfig = configService.getById(config.getId());
            if (existingConfig == null) {
                throw new BizException(404, "配置项不存在：" + config.getConfigKey());
            }

            // 4. 关键修复：防止脱敏数据覆盖真实数据
            // 如果前端传回的 value 包含 "******"，说明用户没有修改该字段，应保持数据库原值
            String newValue = config.getConfigValue();
            if (newValue != null && newValue.contains("******")) {
                // 用户未修改，使用数据库中的旧值
                config.setConfigValue(existingConfig.getConfigValue());
            }

            // 5. 设置更新人和更新时间
            config.setUpdateBy(currentUserId);
            config.setUpdateTime(LocalDateTime.now());

            // 6. 更新配置
            configService.updateById(config);
        }

        return Result.success("系统配置更新成功");
    }
}
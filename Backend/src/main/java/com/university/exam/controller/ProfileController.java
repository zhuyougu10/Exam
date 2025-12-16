package com.university.exam.controller;

import com.university.exam.common.exception.BizException;
import com.university.exam.common.request.UpdatePasswordRequest;
import com.university.exam.common.request.UpdateProfileRequest;
import com.university.exam.common.result.Result;
import com.university.exam.entity.Dept;
import com.university.exam.entity.User;
import com.university.exam.service.DeptService;
import com.university.exam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 个人中心控制器
 * 处理当前登录用户的个人信息查询、修改和密码变更
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-10
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final DeptService deptService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 获取个人完整信息
     * GET /api/profile
     */
    @GetMapping
    public Result<?> getProfile() {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 构建响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        data.put("deptId", user.getDeptId());
        data.put("createTime", user.getCreateTime());

        // 获取部门名称
        if (user.getDeptId() != null) {
            Dept dept = deptService.getById(user.getDeptId());
            data.put("deptName", dept != null ? dept.getDeptName() : "未知部门");
        }

        return Result.success(data, "获取个人信息成功");
    }

    /**
     * 修改个人基本资料
     * PUT /api/profile
     */
    @PutMapping
    public Result<?> updateProfile(@RequestBody UpdateProfileRequest req) {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 更新字段（真实姓名不允许用户自行修改）
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        
        user.setUpdateBy(userId);
        user.setUpdateTime(LocalDateTime.now());

        userService.updateById(user);
        return Result.success("个人资料更新成功");
    }

    /**
     * 修改密码
     * PUT /api/profile/password
     */
    @PutMapping("/password")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest req) {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "旧密码错误");
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdateBy(userId);
        user.setUpdateTime(LocalDateTime.now());

        userService.updateById(user);
        return Result.success("密码修改成功，请重新登录");
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new BizException(401, "登录已失效");
        }
    }
}
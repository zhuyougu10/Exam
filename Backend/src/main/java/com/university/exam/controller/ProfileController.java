package com.university.exam.controller;

import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.User;
import com.university.exam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 个人中心控制器
 * 处理用户个人信息相关请求
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 获取当前登录用户的详细信息
     *
     * @return 当前用户详细信息
     */
    @GetMapping
    public Result<?> getCurrentUserInfo() {
        // 1. 获取当前登录用户ID
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. 从数据库查询用户详细信息
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 3. 构建用户详细信息响应（密码不返回）
        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "avatar", user.getAvatar(),
                "role", user.getRole().intValue(),
                "deptId", user.getDeptId(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "status", user.getStatus(),
                "createTime", user.getCreateTime()
        );

        return Result.success(userInfo, "获取用户信息成功");
    }

    /**
     * 更新当前用户的基本信息
     *
     * @param userUpdateInfo 更新的用户信息
     * @return 更新结果
     */
    @PutMapping
    public Result<?> updateCurrentUserInfo(@RequestBody User userUpdateInfo) {
        // 1. 获取当前登录用户ID
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. 检查用户是否存在
        User existingUser = userService.getById(currentUserId);
        if (existingUser == null) {
            throw new BizException(404, "用户不存在");
        }

        // 3. 更新用户信息
        User updatedUser = new User();
        updatedUser.setId(currentUserId);
        updatedUser.setRealName(userUpdateInfo.getRealName());
        updatedUser.setAvatar(userUpdateInfo.getAvatar());
        updatedUser.setEmail(userUpdateInfo.getEmail());
        updatedUser.setPhone(userUpdateInfo.getPhone());
        updatedUser.setUpdateBy(currentUserId);
        updatedUser.setUpdateTime(LocalDateTime.now());

        // 4. 数据更新
        userService.updateById(updatedUser);

        return Result.success("用户信息更新成功");
    }

    /**
     * 修改当前用户密码
     *
     * @param passwordUpdateInfo 密码更新信息
     * @return 更新结果
     */
    @PutMapping("/password")
    public Result<?> updateCurrentUserPassword(@RequestBody Map<String, String> passwordUpdateInfo) {
        // 1. 获取当前登录用户ID
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. 参数验证
        String oldPassword = passwordUpdateInfo.get("oldPassword");
        String newPassword = passwordUpdateInfo.get("newPassword");

        if (!StringUtils.hasText(oldPassword)) {
            throw new BizException(400, "原密码不能为空");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new BizException(400, "新密码不能为空");
        }

        // 3. 检查密码复杂度（至少8位，包含字母和数字）
        if (newPassword.length() < 8) {
            throw new BizException(400, "新密码长度不能少于8位");
        }
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BizException(400, "新密码必须包含字母和数字");
        }

        // 4. 从数据库查询用户信息
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 5. 验证原密码
        boolean passwordMatch = passwordEncoder.matches(oldPassword, user.getPassword());
        if (!passwordMatch) {
            throw new BizException(400, "原密码错误");
        }

        // 6. 加密新密码并更新
        User updatedUser = new User();
        updatedUser.setId(currentUserId);
        updatedUser.setPassword(passwordEncoder.encode(newPassword));
        updatedUser.setUpdateBy(currentUserId);
        updatedUser.setUpdateTime(LocalDateTime.now());

        userService.updateById(updatedUser);

        return Result.success("密码修改成功");
    }
}
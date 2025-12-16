package com.university.exam.controller;

import com.university.exam.common.exception.BizException;
import com.university.exam.common.request.LoginRequest;
import com.university.exam.common.result.Result;
import com.university.exam.common.utils.JwtUtils;
import com.university.exam.entity.User;
import com.university.exam.service.TokenService;
import com.university.exam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、注销等认证相关请求
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 用户登录接口
     *
     * @param loginRequest 登录请求参数
     * @return 登录结果
     * @throws BizException 业务异常
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. 从数据库查询指定username的用户记录
        User user = userService.lambdaQuery()
                .eq(User::getUsername, loginRequest.getUsername())
                .one();

        // 2. 检查用户是否存在
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BizException(401, "用户已被禁用");
        }

        // 4. 使用BCrypt验证密码
        boolean passwordMatch = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!passwordMatch) {
            throw new BizException(401, "密码错误");
        }

        // 5. 生成JWT Token并存入Redis
        String token = tokenService.createToken(
                user.getId(),
                user.getRole().intValue(), // Byte转Integer
                user.getDeptId() // 已经是Long类型
        );

        // 6. 构建响应数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("role", user.getRole().intValue()); // Byte转Integer
        userInfo.put("deptId", user.getDeptId());
        userInfo.put("name", user.getRealName());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("userInfo", userInfo);

        // 7. 返回成功响应
        return Result.success(responseData, "登录成功");
    }

    /**
     * 获取当前登录用户详细信息
     *
     * @return 用户详细信息
     * @throws BizException 业务异常
     */
    @GetMapping("/info")
    public Result<?> getInfo() {
        // 1. 从SecurityContextHolder获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(401, "未授权访问");
        }

        // 2. 获取当前登录用户ID
        Long userId = (Long) authentication.getPrincipal();

        // 3. 从数据库查询用户详细信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 4. 构建用户详细信息响应
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole().intValue()); // Byte转Integer
        userInfo.put("deptId", user.getDeptId());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("status", user.getStatus());
        userInfo.put("createTime", user.getCreateTime());

        // 5. 返回成功响应
        return Result.success(userInfo, "获取用户信息成功");
    }

    /**
     * 用户登出接口
     * 将Token加入黑名单，使其失效
     *
     * @param request HTTP请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        // 1. 从请求头获取Token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 2. 将Token从Redis中删除并加入黑名单
            tokenService.removeToken(token);
            log.info("用户登出成功");
        }
        
        // 3. 清除SecurityContext
        SecurityContextHolder.clearContext();
        
        return Result.success(null, "登出成功");
    }
}

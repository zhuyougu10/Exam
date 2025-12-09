package com.university.exam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.exam.common.result.Result;
import com.university.exam.entity.User;
import com.university.exam.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController单元测试
 * 验证getInfo()方法的功能
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@WebMvcTest(AuthController.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试getInfo()方法 - 未授权访问
     * 预期结果：返回401错误
     *
     * @throws Exception 测试异常
     */
    @Test
    void testGetInfo_Unauthorized() throws Exception {
        // 1. 清空SecurityContext
        SecurityContextHolder.clearContext();

        // 2. 发送GET请求
        ResultActions resultActions = mockMvc.perform(get("/api/auth/info")
                .contentType(MediaType.APPLICATION_JSON));

        // 3. 验证响应
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("未授权访问"));
    }

    /**
     * 测试getInfo()方法 - 用户不存在
     * 预期结果：返回404错误
     *
     * @throws Exception 测试异常
     */
    @Test
    void testGetInfo_UserNotFound() throws Exception {
        // 1. 模拟认证信息
        Long userId = 1L;
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 2. 模拟UserService返回null
        when(userService.getById(userId)).thenReturn(null);

        // 3. 发送GET请求
        ResultActions resultActions = mockMvc.perform(get("/api/auth/info")
                .contentType(MediaType.APPLICATION_JSON));

        // 4. 验证响应
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    /**
     * 测试getInfo()方法 - 成功获取用户信息
     * 预期结果：返回用户详细信息
     *
     * @throws Exception 测试异常
     */
    @Test
    void testGetInfo_Success() throws Exception {
        // 1. 模拟认证信息
        Long userId = 1L;
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 2. 创建测试用户数据
        User user = new User();
        user.setId(userId);
        user.setUsername("test");
        user.setRealName("测试用户");
        user.setRole((byte) 1);
        user.setDeptId(1L);
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setAvatar("https://example.com/avatar.jpg");
        user.setStatus((byte) 1);

        // 3. 模拟UserService返回用户数据
        when(userService.getById(userId)).thenReturn(user);

        // 4. 发送GET请求
        ResultActions resultActions = mockMvc.perform(get("/api/auth/info")
                .contentType(MediaType.APPLICATION_JSON));

        // 5. 验证响应
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("获取用户信息成功"))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value("test"))
                .andExpect(jsonPath("$.data.realName").value("测试用户"))
                .andExpect(jsonPath("$.data.role").value(1))
                .andExpect(jsonPath("$.data.deptId").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.avatar").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    /**
     * 测试后清理SecurityContext
     */
    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
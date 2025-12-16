package com.university.exam.service;

import com.university.exam.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token服务类
 * 管理JWT Token的生成、存储和验证
 * 使用Redis存储Token实现：
 * 1. Token黑名单（登出后Token失效）
 * 2. 单点登录控制（一个用户只能一个有效Token）
 * 3. Token续期
 *
 * @author exam-system
 * @since 2025-12-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisService redisService;
    private final JwtUtils jwtUtils;

    /**
     * Token在Redis中的前缀
     */
    private static final String TOKEN_PREFIX = "auth:token:";

    /**
     * 用户Token映射前缀（用于单点登录控制）
     */
    private static final String USER_TOKEN_PREFIX = "auth:user:";

    /**
     * Token黑名单前缀
     */
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    @Value("${jwt.expire:7200}")
    private long tokenExpire;

    /**
     * 创建Token并存入Redis
     *
     * @param userId 用户ID
     * @param role   角色
     * @param deptId 部门ID
     * @return Token
     */
    public String createToken(Long userId, Integer role, Long deptId) {
        // 1. 生成JWT Token
        String token = jwtUtils.generateToken(userId, role, deptId);

        // 2. 将旧Token加入黑名单（实现单点登录）
        String oldToken = (String) redisService.get(USER_TOKEN_PREFIX + userId);
        if (oldToken != null) {
            // 将旧Token加入黑名单
            addToBlacklist(oldToken);
        }

        // 3. 存储用户信息到Redis
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("role", role);
        userInfo.put("deptId", deptId);
        userInfo.put("loginTime", System.currentTimeMillis());

        redisService.set(TOKEN_PREFIX + token, userInfo, tokenExpire, TimeUnit.SECONDS);

        // 4. 存储用户-Token映射（用于单点登录控制）
        redisService.set(USER_TOKEN_PREFIX + userId, token, tokenExpire, TimeUnit.SECONDS);

        log.info("用户 {} 登录成功，Token已存入Redis", userId);
        return token;
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        // 1. 检查Token是否在黑名单中
        if (isBlacklisted(token)) {
            log.warn("Token在黑名单中");
            return false;
        }

        // 2. 检查Token是否在Redis中存在
        if (!Boolean.TRUE.equals(redisService.hasKey(TOKEN_PREFIX + token))) {
            log.warn("Token不存在于Redis中");
            return false;
        }

        // 3. 验证JWT本身的有效性
        return jwtUtils.validateToken(token);
    }

    /**
     * 获取Token中存储的用户信息
     *
     * @param token JWT Token
     * @return 用户信息Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String token) {
        Object info = redisService.get(TOKEN_PREFIX + token);
        if (info instanceof Map) {
            return (Map<String, Object>) info;
        }
        return null;
    }

    /**
     * 刷新Token过期时间
     *
     * @param token JWT Token
     */
    public void refreshToken(String token) {
        if (Boolean.TRUE.equals(redisService.hasKey(TOKEN_PREFIX + token))) {
            redisService.expire(TOKEN_PREFIX + token, tokenExpire, TimeUnit.SECONDS);

            // 同时刷新用户-Token映射
            Map<String, Object> userInfo = getUserInfo(token);
            if (userInfo != null) {
                Long userId = ((Number) userInfo.get("userId")).longValue();
                redisService.expire(USER_TOKEN_PREFIX + userId, tokenExpire, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 删除Token（用户登出）
     *
     * @param token JWT Token
     */
    public void removeToken(String token) {
        // 1. 获取用户信息
        Map<String, Object> userInfo = getUserInfo(token);

        // 2. 删除Token
        redisService.delete(TOKEN_PREFIX + token);

        // 3. 删除用户-Token映射
        if (userInfo != null) {
            Long userId = ((Number) userInfo.get("userId")).longValue();
            redisService.delete(USER_TOKEN_PREFIX + userId);
        }

        // 4. 将Token加入黑名单
        addToBlacklist(token);

        log.info("Token已删除并加入黑名单");
    }

    /**
     * 将Token加入黑名单
     *
     * @param token JWT Token
     */
    private void addToBlacklist(String token) {
        // 黑名单保留时间与Token过期时间一致
        redisService.set(BLACKLIST_PREFIX + token, "1", tokenExpire, TimeUnit.SECONDS);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param token JWT Token
     * @return 是否在黑名单中
     */
    private boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisService.hasKey(BLACKLIST_PREFIX + token));
    }

    /**
     * 强制用户下线（管理员功能）
     *
     * @param userId 用户ID
     */
    public void forceLogout(Long userId) {
        String token = (String) redisService.get(USER_TOKEN_PREFIX + userId);
        if (token != null) {
            removeToken(token);
            log.info("用户 {} 已被强制下线", userId);
        }
    }
}

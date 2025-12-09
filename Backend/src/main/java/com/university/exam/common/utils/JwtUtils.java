package com.university.exam.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类，用于生成和验证JWT Token
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
@Slf4j
@Component
public class JwtUtils {
    /**
     * JWT签名密钥
     */
    @Value("${jwt.secret:exam_system_secret_key}")
    private String secret;

    /**
     * JWT过期时间（秒）
     */
    @Value("${jwt.expire:7200}")
    private long expire;

    /**
     * 生成JWT Token
     *
     * @param userId 用户ID
     * @param role 角色
     * @param deptId 部门ID
     * @return JWT Token
     */
    public String generateToken(Long userId, Integer role, Long deptId) {
        // 创建签名密钥
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        // 计算过期时间
        Date expireDate = new Date(System.currentTimeMillis() + expire * 1000);

        // 生成JWT Token
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("userId", userId)
                .claim("role", role)
                .claim("deptId", deptId)
                .setExpiration(expireDate)
                .setIssuedAt(new Date())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT Token
     *
     * @param token JWT Token
     * @return JWT Claims
     * @throws JwtException JWT解析异常
     */
    public Claims parseToken(String token) throws JwtException {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证JWT Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Token签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token参数错误: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token验证失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取角色
     *
     * @param token JWT Token
     * @return 角色
     */
    public Integer getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", Integer.class);
    }
}
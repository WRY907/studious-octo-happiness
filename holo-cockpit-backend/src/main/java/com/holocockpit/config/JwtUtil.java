package com.holocockpit.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（HS256）
 * claims: userId / username / role，有效期 24 小时
 */
public final class JwtUtil {

    /**
     * 签名密钥
     */
    private static final String SECRET = "huawei-cockpit-secret-2026";

    /**
     * 有效期：24 小时
     */
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000L;

    private JwtUtil() {
    }

    /**
     * 生成令牌
     */
    public static String createToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRE_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    /**
     * 解析令牌，令牌无效或已过期时抛出异常
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }
}

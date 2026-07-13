package com.upc.computer.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：Controller 生成 token，过滤器/接口校验 token
 */
@Component
public class JwtUtil {

    @Value("${auth.jwt-secret}")
    private String jwtSecret;

    @Value("${auth.jwt-expire-hours:24}")
    private long jwtExpireHours;

    public long getExpireSeconds() {
        return jwtExpireHours * 3600L;
    }

    public String generateToken(Long userId, String username, Long roleId, String roleCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roleId", roleId);
        claims.put("roleCode", roleCode);
        Date now = new Date();
        Date expire = new Date(now.getTime() + getExpireSeconds() * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setId(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration() != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractTokenFromHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue) || !headerValue.startsWith(JwtConstants.JWT_PREFIX)) {
            return null;
        }
        return headerValue.substring(JwtConstants.JWT_PREFIX.length()).trim();
    }

    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

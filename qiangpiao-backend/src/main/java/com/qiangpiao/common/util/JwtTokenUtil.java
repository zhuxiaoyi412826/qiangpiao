package com.qiangpiao.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具：生成 / 解析 / 校验（前后端分离无状态登录）。
 */
@Slf4j
public class JwtTokenUtil {

    private final String secret;
    private final long expirationMillis;

    public JwtTokenUtil(String secret, long expirationSeconds) {
        this.secret = secret;
        this.expirationMillis = expirationSeconds * 1000;
    }

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败：{}", e.getMessage());
            return false;
        }
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public Long getUserId(String token) {
        Object uid = parse(token).get("uid");
        return uid == null ? null : ((Number) uid).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles == null ? java.util.Collections.emptyList() : (List<String>) roles;
    }

    public Date getExpiration(String token) {
        return parse(token).getExpiration();
    }
}

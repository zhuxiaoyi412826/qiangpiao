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
import java.util.UUID;

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
                // jti：token 唯一 ID，登出 / 踢下线时把它放进 Redis 黑名单即可精准失效单端
                .setId(UUID.randomUUID().toString().replace("-", ""))
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

    /**
     * token 唯一 ID（jti）。改造前签发的老 token 没有这个字段，返回 null，
     * 调用方需按「不受黑名单约束」处理，保证老 token 用到自然过期。
     */
    public String getJti(String token) {
        try {
            return parse(token).getId();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** token 剩余有效秒数（已过期返回 0）；解析失败时回退为配置的有效期 */
    public long remainingSeconds(String token) {
        try {
            long ms = parse(token).getExpiration().getTime() - System.currentTimeMillis();
            return ms <= 0 ? 0 : ms / 1000;
        } catch (JwtException | IllegalArgumentException e) {
            return expirationMillis / 1000;
        }
    }
}

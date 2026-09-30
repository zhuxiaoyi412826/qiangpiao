package com.qiangpiao.service;

/**
 * Token 生命周期管理：无状态 JWT 的「登出 / 踢下线 / 封号立即失效」。
 * <p>
 * JWT 签发后无法单独作废，这里用 Redis 黑名单补上这个能力：
 * 登出时把 token 的 jti 写进黑名单（TTL = 剩余有效期，过期自动清理，不留垃圾），
 * JwtAuthenticationFilter 每次认证后查一次黑名单，命中即视为未登录。
 */
public interface TokenService {

    /**
     * 登录 / 注册签发 token 后登记 jti，供「踢下线」批量拉黑。
     */
    void register(Long userId, String token);

    /**
     * 该 token 是否已被拉黑（已登出 / 被踢 / 账号被封）。
     * Redis 不可用时返回 false：宁可放行，也不能因为缓存抖动把所有人都踢下线。
     */
    boolean blacklisted(String jti);

    /**
     * 拉黑单个 token（登出）。
     *
     * @return true 表示成功失效；false 表示 token 无 jti 或已过期
     */
    boolean blacklistToken(String token);

    /**
     * 踢下线：拉黑该用户当前所有有效 token（多端一起下线）。
     *
     * @return 被拉黑的 token 数量
     */
    int kickUser(Long userId);
}

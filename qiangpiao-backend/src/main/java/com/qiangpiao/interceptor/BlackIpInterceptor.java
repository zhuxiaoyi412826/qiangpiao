package com.qiangpiao.interceptor;

import com.qiangpiao.common.util.IpUtils;
import com.qiangpiao.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * IP 黑名单拦截器：命中黑名单的请求在入口直接拒绝，不进入业务层。
 * <p>
 * 与业务层 {@code assertSeckillAllowed} 形成双保险：拦截器挡全站，业务层挡最新拉黑（Redis 已写、拦截器刚读过的情况）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlackIpInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = IpUtils.getIp(request);
        if (!rateLimitService.isBlackIp(ip)) {
            return true;
        }
        log.warn("黑名单 IP 被拦截：ip={}, uri={}", ip, request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new String(
                "{\"code\":4021,\"message\":\"该 IP 已被限制访问，如有疑问请联系客服\",\"data\":null}"
                        .getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        return false;
    }
}

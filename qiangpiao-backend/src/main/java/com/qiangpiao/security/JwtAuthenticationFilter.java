package com.qiangpiao.security;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.context.UserContext;
import com.qiangpiao.common.util.IpUtils;
import com.qiangpiao.common.util.JwtTokenUtil;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器：解析请求头 Token -> 构建认证对象 -> 放入 SecurityContext 与 UserContext。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtTokenUtil.validate(token)) {
                // 已登出 / 被踢下线 / 账号被封：token 本身没过期，但黑名单里有它的 jti
                String jti = jwtTokenUtil.getJti(token);
                if (tokenService.blacklisted(jti)) {
                    // 不放认证：受保护接口会被 authenticationEntryPoint 拦成 401，前端跳登录页
                    log.info("token 已失效（登出 / 强制下线）：jti={}", jti);
                } else {
                    Long userId = jwtTokenUtil.getUserId(token);
                    String username = jwtTokenUtil.getUsername(token);
                    LoginUser loginUser = new LoginUser(userId, username, jwtTokenUtil.getRoles(token));
                    loginUser.setIp(IpUtils.getIp(request));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    UserContext.set(new UserContext.LoginUserHolder(userId, username, loginUser.getIp()));
                    // 登录用户写进 MDC：之后这条请求的所有日志都带 userId（logback %X{userId}）
                    TraceContext.putUser(userId);
                    log.debug("JWT 认证成功：userId={}, username={}", userId, username);
                }
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            // 过滤器是最外层，请求结束统一清 MDC，防止容器线程复用导致 traceId 串号
            TraceContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(Constants.TOKEN_PREFIX)) {
            return header.substring(Constants.TOKEN_PREFIX.length());
        }
        // 兼容 query 方式
        String param = request.getParameter("token");
        return StringUtils.hasText(param) ? param : null;
    }
}

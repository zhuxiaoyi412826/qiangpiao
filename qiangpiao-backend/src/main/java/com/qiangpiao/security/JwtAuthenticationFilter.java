package com.qiangpiao.security;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.context.UserContext;
import com.qiangpiao.common.util.IpUtils;
import com.qiangpiao.common.util.JwtTokenUtil;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtTokenUtil.validate(token)) {
                Long userId = jwtTokenUtil.getUserId(token);
                String username = jwtTokenUtil.getUsername(token);
                LoginUser loginUser = new LoginUser(userId, username, jwtTokenUtil.getRoles(token));
                loginUser.setIp(IpUtils.getIp(request));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserContext.set(new UserContext.LoginUserHolder(userId, username, loginUser.getIp()));
                log.debug("JWT 认证成功：userId={}, username={}", userId, username);
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
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

package com.qiangpiao.config;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.JwtTokenUtil;
import com.qiangpiao.security.JwtAuthenticationFilter;
import com.qiangpiao.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 配置（前后端分离：JWT + 无状态）。
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /** 无需登录即可访问的接口 */
    private static final String[] PERMIT_ALL = {
            "/api/auth/**",
            "/api/trains/**",
            "/api/stations/**",
            "/api/announcements/**",
            "/api/health/**",
            // 支付回调由渠道服务器调用，无 Token，靠验签保证来源可信
            "/api/payments/notify",
            "/doc.html",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/v2/api-docs/**",
            "/webjars/**",
            "/druid/**",
            "/error/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(jwtSecret, jwtExpiration);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                                           TokenService tokenService) {
        // TokenService：每次认证后查一次黑名单，让「登出 / 踢下线」能立即生效
        return new JwtAuthenticationFilter(jwtTokenUtil, tokenService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                .csrf().disable()
                .cors().configurationSource(corsConfigurationSource()).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .headers().frameOptions().disable().and()
                .authorizeHttpRequests(auth -> auth
                        .antMatchers(PERMIT_ALL).permitAll()
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling()
                .authenticationEntryPoint((request, response, ex) -> writeJson(response, ResultCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, ex) -> writeJson(response, ResultCode.FORBIDDEN))
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout().disable()
                .formLogin().disable()
                .httpBasic().disable();
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList(Constants.TOKEN_HEADER, "X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeJson(javax.servlet.http.HttpServletResponse response, ResultCode code) {
        response.setStatus(code == ResultCode.UNAUTHORIZED ? 401 : 403);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(R.fail(code.getCode(), code.getMessage())));
        } catch (Exception ignored) {
            // ignore
        }
    }
}

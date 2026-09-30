package com.qiangpiao.controller;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.IpUtils;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.dto.LoginDTO;
import com.qiangpiao.dto.RegisterDTO;
import com.qiangpiao.service.AuthService;
import com.qiangpiao.vo.LoginVO;
import com.qiangpiao.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证接口（登录 / 注册 / 当前用户）。
 */
@RestController
@RequestMapping("/api/auth")
@Api(tags = "认证中心")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ApiOperation("登录")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        return R.ok(authService.login(loginDTO, IpUtils.getIp(request)));
    }

    @PostMapping("/register")
    @ApiOperation("注册")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO registerDTO, HttpServletRequest request) {
        return R.ok(authService.register(registerDTO, IpUtils.getIp(request)));
    }

    @GetMapping("/info")
    @ApiOperation("当前登录用户")
    public R<UserVO> info() {
        return R.ok(authService.currentUser(SecurityUtils.currentUserId()));
    }

    @PostMapping("/logout")
    @ApiOperation("登出（token 进黑名单，立即失效）")
    public R<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return R.ok();
    }

    /** 取请求里的 token：优先 Authorization 头，其次 query（前端 / 压测脚本兼容） */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(Constants.TOKEN_PREFIX)) {
            return header.substring(Constants.TOKEN_PREFIX.length());
        }
        String param = request.getParameter("token");
        return StringUtils.hasText(param) ? param : null;
    }
}

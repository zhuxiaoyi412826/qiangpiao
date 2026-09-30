package com.qiangpiao.service;

import com.qiangpiao.bo.LoginUserBO;
import com.qiangpiao.dto.LoginDTO;
import com.qiangpiao.dto.RegisterDTO;
import com.qiangpiao.vo.LoginVO;
import com.qiangpiao.vo.UserVO;

/**
 * 认证服务。
 */
public interface AuthService {

    /**
     * 登录：校验账号密码 -> 签发 JWT
     */
    LoginVO login(LoginDTO loginDTO, String ip);

    /**
     * 注册
     */
    LoginVO register(RegisterDTO registerDTO, String ip);

    /**
     * 当前登录用户
     */
    UserVO currentUser(Long userId);

    /**
     * 登出：把当前 token 拉黑，立即失效（JWT 本身无法作废，靠黑名单实现）。
     *
     * @param token 原始 token（不带 Bearer 前缀）
     */
    void logout(String token);

    /**
     * 登录用户业务对象（供其他 Service 调用）
     */
    LoginUserBO getLoginUserBO(Long userId);
}

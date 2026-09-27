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
     * 登录用户业务对象（供其他 Service 调用）
     */
    LoginUserBO getLoginUserBO(Long userId);
}

package com.qiangpiao.service.impl;

import com.qiangpiao.bo.LoginUserBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.JwtTokenUtil;
import com.qiangpiao.dataobject.UserDO;
import com.qiangpiao.dto.LoginDTO;
import com.qiangpiao.dto.RegisterDTO;
import com.qiangpiao.mapper.UserMapper;
import com.qiangpiao.service.AuthService;
import com.qiangpiao.vo.LoginVO;
import com.qiangpiao.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public LoginVO login(LoginDTO loginDTO, String ip) {
        UserDO user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == Constants.USER_STATUS_DISABLED) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        log.info("用户登录成功：userId={}, username={}, ip={}", user.getId(), user.getUsername(), ip);
        return buildLoginVO(user);
    }

    @Override
    public LoginVO register(RegisterDTO registerDTO, String ip) {
        if (userMapper.selectByUsername(registerDTO.getUsername()) != null) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        UserDO user = new UserDO();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setIdCard(registerDTO.getIdCard());
        user.setRole("ROLE_USER");
        user.setStatus(Constants.USER_STATUS_NORMAL);
        userMapper.insert(user);
        log.info("用户注册成功：userId={}, username={}, ip={}", user.getId(), user.getUsername(), ip);
        return buildLoginVO(user);
    }

    @Override
    public UserVO currentUser(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return UserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .idCard(maskIdCard(user.getIdCard()))
                .roles(Collections.singletonList(user.getRole()))
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }

    @Override
    public LoginUserBO getLoginUserBO(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        List<String> roles = Collections.singletonList(user.getRole() == null ? "ROLE_USER" : user.getRole());
        return LoginUserBO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .roles(roles)
                .build();
    }

    private LoginVO buildLoginVO(UserDO user) {
        List<String> roles = Collections.singletonList(user.getRole() == null ? "ROLE_USER" : user.getRole());
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), roles);
        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roles)
                .build();
    }

    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}

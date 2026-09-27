package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("登录出参")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("JWT Token")
    private String token;
    @ApiModelProperty("Token 类型")
    private String tokenType = "Bearer";
    @ApiModelProperty("过期时间（秒）")
    private Long expiresIn;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("真实姓名")
    private String realName;
    @ApiModelProperty("角色列表")
    private List<String> roles;
}

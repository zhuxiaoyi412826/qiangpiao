package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("用户出参")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("真实姓名")
    private String realName;
    @ApiModelProperty("手机号")
    private String phone;
    @ApiModelProperty("身份证号（脱敏）")
    private String idCard;
    @ApiModelProperty("角色")
    private List<String> roles;
    @ApiModelProperty("状态：1-正常 0-禁用")
    private Integer status;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}

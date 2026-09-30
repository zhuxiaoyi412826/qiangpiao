package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 图形验证码出参。
 */
@Data
@ApiModel("图形验证码")
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("验证码 ID，提交时原样带回")
    private String captchaId;
    @ApiModelProperty("图片 Base64（data:image/png;base64, 前缀）")
    private String image;
    @ApiModelProperty("有效时长（秒）")
    private Long expireSeconds;
}

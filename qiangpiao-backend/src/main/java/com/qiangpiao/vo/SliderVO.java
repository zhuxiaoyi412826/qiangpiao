package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 滑块验证码出参：只给背景图与拼图块，缺口 x 坐标留在服务端。
 */
@Data
@ApiModel("滑块验证码")
public class SliderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("滑块 ID，提交时原样带回")
    private String sliderId;
    @ApiModelProperty("背景图 Base64（带缺口阴影）")
    private String background;
    @ApiModelProperty("拼图块 Base64")
    private String block;
    @ApiModelProperty("拼图块纵向位置（px，前端按此定位）")
    private Integer blockY;
    @ApiModelProperty("拼图块宽度（px）")
    private Integer blockWidth;
    @ApiModelProperty("拼图块高度（px）")
    private Integer blockHeight;
    @ApiModelProperty("背景图宽度（px）")
    private Integer width;
    @ApiModelProperty("背景图高度（px）")
    private Integer height;
    @ApiModelProperty("有效时长（秒）")
    private Long expireSeconds;
}

package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 秒杀抢票入参 DTO。
 */
@Data
@ApiModel("秒杀抢票入参")
public class SeckillDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "车次 ID 不能为空")
    @ApiModelProperty(value = "车次ID", required = true, example = "1")
    private Long trainId;

    @NotNull(message = "席别不能为空")
    @Min(value = 1, message = "席别不合法")
    @Max(value = 3, message = "席别不合法")
    @ApiModelProperty(value = "席别：1-商务座 2-一等座 3-二等座", required = true, example = "3")
    private Integer seatType;

    @ApiModelProperty(value = "指定座位ID（不传则由系统自动分配）；单张票场景使用")
    private Long seatId;

    /**
     * 批量购票乘客列表：一次最多 9 张（每位乘客一张票）。
     * 传了它则忽略 passengerId / passengerName / idCard。
     */
    @Valid
    @Size(max = 9, message = "一次最多购买 9 张票")
    @ApiModelProperty(value = "购票乘客列表（一次最多 9 位，每位一张票）")
    private List<PassengerItemDTO> passengers;

    /** 批量购票时指定的座位（数量需与乘客数一致，不传则系统自动分配） */
    @ApiModelProperty(value = "指定的座位ID列表（数量需与乘客数一致，不传自动分配）")
    private List<Long> seatIds;

    /**
     * 常用乘车人 ID：传它时不必再传姓名 / 身份证，服务端按本人常用乘车人解密取明文。
     * 优先于 passengerName + idCard。
     */
    @ApiModelProperty(value = "常用乘车人ID（传了则无需再传姓名与身份证号）")
    private Long passengerId;

    @ApiModelProperty(value = "乘客姓名；不传 passengerId 时必填", example = "张三")
    private String passengerName;

    @Pattern(regexp = "^(\\d{17}[0-9Xx]|\\d{15})$", message = "身份证号格式不正确")
    @ApiModelProperty(value = "身份证号（明文）；不传 passengerId 时必填")
    private String idCard;

    @ApiModelProperty(value = "手机号（明文，选填；服务端做格式校验，仅本次购票使用，不落库）")
    private String phone;

    /* ========== 人机验证（抢票前置，二选一） ========== */
    @ApiModelProperty(value = "图形验证码ID（GET /api/captcha/image 获取）")
    private String captchaId;

    @ApiModelProperty(value = "图形验证码文本")
    private String captchaCode;

    @ApiModelProperty(value = "滑块验证码ID（GET /api/captcha/slider 获取）")
    private String sliderId;

    @ApiModelProperty(value = "滑块拖动到的 x 坐标（px）")
    private Integer sliderX;

    /* ========== 区间票（可选，不传 = 买全程票） ========== */
    @ApiModelProperty(value = "上车站名称（区间票，中途上车）；不传视为从始发站上车")
    private String fromStation;

    @ApiModelProperty(value = "下车站名称（区间票，中途下车）；不传视为到终点站下车")
    private String toStation;
}

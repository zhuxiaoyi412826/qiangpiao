package com.qiangpiao.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 后台订单查询入参：订单号 / 手机号 / 乘客 / 状态 / 下单日期区间。
 */
@Data
public class AdminOrderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    /** 手机号（明文入参） */
    private String phone;
    /** 由 Service 计算后填入：手机号密文，用于等值匹配 */
    private String phoneCipher;
    private String passengerName;
    /** 订单状态：0-待支付 1-已支付 2-已取消 3-已退票 4-已超时 5-已改签 */
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 由 Service 计算后填入，供 MyBatis 分页使用 */
    private Long offset;
    private Long limit;
}

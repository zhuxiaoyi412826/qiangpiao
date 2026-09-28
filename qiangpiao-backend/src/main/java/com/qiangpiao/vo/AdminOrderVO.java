package com.qiangpiao.vo;

import com.qiangpiao.dataobject.TicketDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台订单出参：车票信息 + 下单用户（手机号）/ 状态文案。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminOrderVO extends TicketDO {

    private static final long serialVersionUID = 1L;

    private String username;
    private String phone;
    private String statusText;
}

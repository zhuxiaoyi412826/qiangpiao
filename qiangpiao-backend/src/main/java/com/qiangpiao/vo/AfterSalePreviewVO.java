package com.qiangpiao.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退票 / 改签费用试算（前端确认前展示，避免用户不知道要扣手续费）。
 */
@Data
public class AfterSalePreviewVO {

    /** 订单号 */
    private String orderNo;
    /** 计费基数：退票为票价，改签为新旧两张票中较低的票价 */
    private BigDecimal baseAmount;
    /** 手续费：退票为退票费，改签为改签费 */
    private BigDecimal fee;
    /** 实退金额（退票）/ 实退差额（改签退差） */
    private BigDecimal refundAmount;
    /** 需补金额（改签补差价，退票时为 0） */
    private BigDecimal payAmount;
    /** 费率档位文案 */
    private String feeRule;
    /** 是否免收手续费 */
    private Boolean free;
    /** 温馨提示（如改签后按原票时间计费、春运 20% 等） */
    private String tip;
}

package com.qiangpiao.service;

import com.qiangpiao.vo.PaymentVO;

import java.util.Map;

/**
 * 支付服务：两阶段支付（发起支付 + 渠道异步回调）。
 * <pre>
 *   1. 发起支付：建 t_payment 支付单（支付中），不扣款、不改订单状态
 *   2. 渠道回调：验签 + 幂等 + 金额校验后，才扣钱包、置订单为已支付
 * </pre>
 */
public interface PaymentService {

    /**
     * 发起支付（第一阶段）：创建支付单并返回，等待渠道异步回调。
     *
     * @param orderNo       订单号
     * @param userId        用户ID
     * @param payType       支付方式
     * @param idempotentKey 幂等键（不传则用 订单号+支付方式）
     * @return 支付单
     */
    PaymentVO createPayment(String orderNo, Long userId, String payType, String idempotentKey);

    /**
     * 查询支付单。
     */
    PaymentVO getByPayNo(String payNo);

    /**
     * 处理渠道回调（第二阶段）：
     * 验签 -> 时间戳窗口 -> nonce 防重放 -> 支付单幂等 -> 金额校验 -> 扣款入账。
     *
     * @param params 回调参数（含 sign）
     * @return true-已接收（终态或处理成功）；失败/验签不通过抛异常
     */
    boolean handleNotify(Map<String, Object> params);

    /**
     * 模拟渠道回调：按约定生成签名并触发回调逻辑（演示 / 联调用）。
     *
     * @param result SUCCESS / FAIL
     */
    boolean mockCallback(String payNo, String result);

    /**
     * 支付成功后的履约（扣款 + 入账 + 订单置为已支付），独立事务。
     * 由回调处理通过 AopContext 代理调用，业务方无需直接调用。
     */
    void settleSuccess(com.qiangpiao.dataobject.PaymentDO payment);

    /**
     * 关闭订单名下所有「支付中」的支付单（订单取消 / 超时）。
     */
    void closeByOrderNo(String orderNo, String reason);
}

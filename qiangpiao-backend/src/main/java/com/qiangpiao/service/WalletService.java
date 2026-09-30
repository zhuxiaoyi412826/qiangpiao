package com.qiangpiao.service;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dto.RechargeDTO;
import com.qiangpiao.vo.WalletFlowVO;
import com.qiangpiao.vo.WalletVO;

import java.math.BigDecimal;

/**
 * 钱包服务：余额查询 / 自定义充值 / 消费扣款 / 零钱流水。
 */
public interface WalletService {

    /**
     * 查询钱包（不存在则初始化）。
     */
    WalletVO getWallet(Long userId);

    /**
     * 自定义金额充值（模拟支付渠道回调成功后入账）。
     */
    WalletVO recharge(Long userId, RechargeDTO dto);

    /**
     * 消费扣款：扣余额并生成一条零钱流水。
     *
     * @param amount 消费金额（正数）
     * @param bizNo  业务单号（订单号）
     * @return 扣款后的余额
     */
    BigDecimal pay(Long userId, BigDecimal amount, String bizNo, String title, String detail);

    /**
     * 退款入账：退票 / 后台人工退票时把票款退回用户钱包。
     *
     * @param amount 退款金额（正数）
     * @return 退款后的余额
     */
    BigDecimal refund(Long userId, BigDecimal amount, String bizNo, String title, String detail);

    /**
     * 幂等退款入账：同一 idempotentKey 只会真正入账一次（退票 / 改签退差必须用它）。
     *
     * @param idempotentKey 幂等键，如 REFUND:订单号
     * @return 退款后的余额（重复调用返回首次入账时的余额快照）
     */
    BigDecimal refundOnce(Long userId, BigDecimal amount, String idempotentKey,
                          String bizNo, String title, String detail);

    /**
     * 平台入账：用户购票付款时，票款进入平台账户（收款方），形成闭环资金流。
     *
     * @return 入账后的平台余额
     */
    BigDecimal creditToPlatform(Long userId, BigDecimal amount, String bizNo, String title, String detail);

    /**
     * 零钱流水分页。
     */
    PageResult<WalletFlowVO> pageFlows(Long userId, Integer pageNum, Integer pageSize);
}

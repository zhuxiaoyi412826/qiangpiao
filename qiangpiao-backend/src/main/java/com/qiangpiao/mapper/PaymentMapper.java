package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.PaymentDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付流水 Mapper。
 */
@Repository
public interface PaymentMapper {

    PaymentDO selectByPayNo(@Param("payNo") String payNo);

    PaymentDO selectByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    /**
     * 取同一幂等键（含重试后缀 key#id）下最新一笔支付单。
     */
    PaymentDO selectLatestByIdempotent(@Param("base") String base);

    List<PaymentDO> selectByOrderNo(@Param("orderNo") String orderNo);

    int insert(PaymentDO payment);

    /** 支付成功：仅支付中 -> 成功，条件更新保证并发回调只有一次生效 */
    int markSuccess(@Param("payNo") String payNo, @Param("tradeNo") String tradeNo);

    /** 支付失败：支付中 / 成功（入账失败回滚）-> 失败 */
    int markFailed(@Param("payNo") String payNo, @Param("reason") String reason);

    /** 关闭支付单：仅支付中 -> 已关闭（订单取消 / 超时） */
    int markClosed(@Param("payNo") String payNo, @Param("reason") String reason);

    /** 按订单关闭所有未终态的支付单 */
    int closeByOrderNo(@Param("orderNo") String orderNo, @Param("reason") String reason);

    /** 回调计数 +1（重复回调也要计数，便于排查渠道重发） */
    int increaseNotifyCount(@Param("payNo") String payNo);

    /**
     * 资金对账专用：扫描时间窗口内「支付成功」的支付单（按支付成功时间，走 idx_status_expire）。
     */
    List<PaymentDO> selectSuccessBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

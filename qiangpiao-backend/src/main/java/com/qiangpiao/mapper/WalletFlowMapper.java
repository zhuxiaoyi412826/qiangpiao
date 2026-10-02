package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.WalletFlowDO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 零钱流水 Mapper（SQL 见 resources/mapper/WalletFlowMapper.xml）。
 */
public interface WalletFlowMapper {

    int insert(WalletFlowDO flow);

    /** 按幂等键查流水：退款防重复入账 */
    WalletFlowDO selectByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    List<WalletFlowDO> selectByUserId(@Param("userId") Long userId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    long countByUserId(@Param("userId") Long userId);

    /** 资金对账专用：按业务单号查全部流水（判断漏记 / 重复记账） */
    List<WalletFlowDO> selectByBizNo(@Param("bizNo") String bizNo);

    /** 资金对账专用：某用户的流水金额累计（应与 t_wallet.balance 相等） */
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);

    /**
     * 资金对账专用：某类流水的金额累计。
     *
     * @param excludeUserId 非空时排除该用户（算用户消费合计时排除平台账户）
     */
    BigDecimal sumAmountByType(@Param("type") Integer type, @Param("excludeUserId") Long excludeUserId);
}

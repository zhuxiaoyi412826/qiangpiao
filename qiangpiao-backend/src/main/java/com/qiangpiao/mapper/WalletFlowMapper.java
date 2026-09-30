package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.WalletFlowDO;
import org.apache.ibatis.annotations.Param;

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
}

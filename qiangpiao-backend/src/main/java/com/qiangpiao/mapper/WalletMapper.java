package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.WalletDO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包 Mapper（SQL 见 resources/mapper/WalletMapper.xml）。
 */
public interface WalletMapper {

    WalletDO selectByUserId(@Param("userId") Long userId);

    /**
     * 初始化钱包（幂等）。
     */
    int insertIgnore(@Param("userId") Long userId);

    /**
     * 充值：余额增加，累计充值累加。
     */
    int increaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 消费：乐观锁 + SQL 层余额判断双重保证，扣款成功返回 1。
     */
    int decreaseBalance(@Param("userId") Long userId,
                        @Param("amount") BigDecimal amount,
                        @Param("version") Integer version);

    /** 资金对账专用：分页扫全量钱包（校验余额与流水累计是否一致） */
    List<WalletDO> selectPage(@Param("offset") long offset, @Param("limit") long limit);

    /** 资金对账专用：钱包总数 */
    long countAll();
}

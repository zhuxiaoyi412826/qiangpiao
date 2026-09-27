package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.WalletFlowDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 零钱流水 Mapper（SQL 见 resources/mapper/WalletFlowMapper.xml）。
 */
public interface WalletFlowMapper {

    int insert(WalletFlowDO flow);

    List<WalletFlowDO> selectByUserId(@Param("userId") Long userId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    long countByUserId(@Param("userId") Long userId);
}

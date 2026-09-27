package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.TicketDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 车票 Mapper：订单关联车次，按「是否发车」筛选（SQL 见 resources/mapper/TicketMapper.xml）。
 */
public interface TicketMapper {

    /**
     * @param history false-未开车的有效票（status=1 且未发车）；true-历史票（已发车的票 + 已取消/退票/超时）
     */
    List<TicketDO> selectTickets(@Param("userId") Long userId,
                                 @Param("history") boolean history,
                                 @Param("offset") long offset,
                                 @Param("limit") long limit);

    long countTickets(@Param("userId") Long userId, @Param("history") boolean history);
}

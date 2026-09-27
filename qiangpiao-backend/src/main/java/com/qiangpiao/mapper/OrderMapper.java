package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.OrderDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper。
 */
@Repository
public interface OrderMapper {

    OrderDO selectById(@Param("id") Long id);

    OrderDO selectByOrderNo(@Param("orderNo") String orderNo);

    List<OrderDO> selectByUserId(@Param("userId") Long userId,
                                 @Param("status") Integer status,
                                 @Param("offset") Long offset,
                                 @Param("limit") Long limit);

    long countByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    int insert(OrderDO order);

    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("status") Integer status,
                     @Param("oldStatus") Integer oldStatus);

    int markPaid(@Param("orderNo") String orderNo, @Param("payTime") LocalDateTime payTime);

    int markCancelled(@Param("orderNo") String orderNo, @Param("cancelTime") LocalDateTime cancelTime);

    /**
     * 批量超时关单（走索引扫描待支付订单）
     */
    List<OrderDO> selectExpiredOrders(@Param("deadline") LocalDateTime deadline, @Param("limit") Long limit);

    int countByTrainAndUser(@Param("trainId") Long trainId, @Param("userId") Long userId);
}

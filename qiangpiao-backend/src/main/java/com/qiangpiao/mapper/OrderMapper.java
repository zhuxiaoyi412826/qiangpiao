package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.OrderDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 查询用户在指定时间区间内已存在的有效订单（待支付 / 已支付），用于「行程运行时间冲突」校验。
     * 区间重叠判定：已有车次发车时刻 &lt; 目标到达时刻 且 已有车次到达时刻 &gt; 目标发车时刻。
     */
    List<Map<String, Object>> selectTripConflicts(@Param("userId") Long userId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}

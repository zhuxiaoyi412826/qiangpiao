package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.SeatDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 座位 Mapper。
 */
@Repository
public interface SeatMapper {

    SeatDO selectById(@Param("id") Long id);

    /**
     * 取一个可售座位（秒杀自动分配座位）
     */
    SeatDO selectAvailableOne(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    List<SeatDO> selectByTrainId(@Param("trainId") Long trainId);

    List<SeatDO> selectByTrainAndType(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    /**
     * 抢占座位（乐观锁，保证并发下只有一个线程成功）
     */
    int lockSeat(@Param("id") Long id, @Param("version") Integer version, @Param("orderNo") String orderNo);

    /**
     * 指定座位下单
     */
    int lockSeatById(@Param("id") Long id, @Param("orderNo") String orderNo);

    /**
     * 释放座位（取消订单 / 下单失败补偿）
     */
    int releaseSeatByOrderNo(@Param("orderNo") String orderNo);

    int countAvailable(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    int insertBatch(@Param("list") List<SeatDO> seats);
}

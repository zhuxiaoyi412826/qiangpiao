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

    /**
     * 取一个可售座位并加行锁（FOR UPDATE SKIP LOCKED）。
     * 并发下各事务跳过对方已锁的行，保证一次买多张时分到的是不同座位。
     *
     * @param carriageNo 优先车厢，为空则不限车厢
     */
    SeatDO selectAvailableForUpdate(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                                    @Param("carriageNo") Integer carriageNo);

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

    /**
     * 锁定座位行（区间票：座位可能已被其它区间占用，status 已是 1，这里只加行锁做冲突检查串行化）。
     */
    @org.apache.ibatis.annotations.Select("SELECT id, train_id, seat_type, carriage_no, seat_no, status, order_no," +
            " version, create_time, update_time FROM t_seat WHERE id = #{id} FOR UPDATE")
    SeatDO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 占用座位（不校验 status）：区间复用场景下座位可能已被其它区间的订单占用，
     * 是否冲突由 t_seat_segment 判定，这里只负责把座位标记为已售。
     */
    @org.apache.ibatis.annotations.Update("UPDATE t_seat SET status = 1, order_no = #{orderNo}," +
            " version = version + 1 WHERE id = #{id}")
    int occupySeat(@Param("id") Long id, @Param("orderNo") String orderNo);

    /**
     * 座位彻底无人占用时置回可售（区间票：最后一个区间被释放后才调用）。
     */
    @org.apache.ibatis.annotations.Update("UPDATE t_seat SET status = 0, order_no = NULL, version = version + 1" +
            " WHERE id = #{id}")
    int freeSeat(@Param("id") Long id);
}

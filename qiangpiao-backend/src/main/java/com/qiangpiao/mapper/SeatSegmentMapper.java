package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.SeatSegmentDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 座位区间占用 Mapper：同一座位可被拆成多段卖给不同乘客。
 */
@Repository
public interface SeatSegmentMapper {

    /**
     * 取一个在 [fromOrder, toOrder) 区间内完全空闲的座位并加行锁（FOR UPDATE SKIP LOCKED）。
     * 冲突判定：已占用区间 [a,b) 与 [fo,to) 冲突 ⇔ a &lt; to 且 fo &lt; b。
     */
    @Select("SELECT s.id, s.train_id, s.seat_type, s.carriage_no, s.seat_no, s.status, s.order_no, s.version," +
            " s.create_time, s.update_time" +
            " FROM t_seat s" +
            " WHERE s.train_id = #{trainId} AND s.seat_type = #{seatType}" +
            " AND NOT EXISTS (SELECT 1 FROM t_seat_segment g WHERE g.seat_id = s.id AND g.status = 1" +
            "   AND g.from_order < #{toOrder} AND #{fromOrder} < g.to_order)" +
            " AND (#{carriageNo} IS NULL OR s.carriage_no = #{carriageNo})" +
            " ORDER BY s.carriage_no, s.seat_no LIMIT 1 FOR UPDATE SKIP LOCKED")
    SeatDO selectFreeForUpdate(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                               @Param("fromOrder") Integer fromOrder, @Param("toOrder") Integer toOrder,
                               @Param("carriageNo") Integer carriageNo);

    /** 指定座位在该区间是否空闲：0 表示无冲突 */
    @Select("SELECT COUNT(1) FROM t_seat_segment WHERE seat_id = #{seatId} AND status = 1" +
            " AND from_order < #{toOrder} AND #{fromOrder} < to_order")
    int countConflict(@Param("seatId") Long seatId, @Param("fromOrder") Integer fromOrder,
                      @Param("toOrder") Integer toOrder);

    @Insert("INSERT INTO t_seat_segment (train_id, seat_id, seat_type, from_order, to_order, order_no, status," +
            " create_time, update_time)" +
            " VALUES (#{trainId}, #{seatId}, #{seatType}, #{fromOrder}, #{toOrder}, #{orderNo}, 1, NOW(), NOW())")
    int insert(SeatSegmentDO segment);

    /** 释放：把该订单占用的区间标记为已释放 */
    @Update("UPDATE t_seat_segment SET status = 0, update_time = NOW() WHERE order_no = #{orderNo} AND status = 1")
    int releaseByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT id, train_id, seat_id, seat_type, from_order, to_order, order_no, status, create_time, update_time" +
            " FROM t_seat_segment WHERE order_no = #{orderNo} AND status = 1")
    List<SeatSegmentDO> selectByOrderNo(@Param("orderNo") String orderNo);

    /** 座位是否还有其它有效占用（用于决定要不要把 seat.status 改回可售） */
    @Select("SELECT COUNT(1) FROM t_seat_segment WHERE seat_id = #{seatId} AND status = 1")
    int countActiveBySeat(@Param("seatId") Long seatId);
}

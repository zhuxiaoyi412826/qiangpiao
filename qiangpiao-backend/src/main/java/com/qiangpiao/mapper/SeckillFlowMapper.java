package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.SeckillFlowDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 秒杀抢票流水 Mapper（t_seckill_flow）。
 */
@Repository
public interface SeckillFlowMapper {

    @Insert("INSERT INTO t_seckill_flow (batch_no, order_no, user_id, train_id, seat_type, passenger_name," +
            " ticket_index, status, queue_seq, client_ip, create_time)" +
            " VALUES (#{batchNo}, #{orderNo}, #{userId}, #{trainId}, #{seatType}, #{passengerName}," +
            " #{ticketIndex}, #{status}, #{queueSeq}, #{clientIp}, NOW())")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = Long.class)
    int insert(SeckillFlowDO flow);

    /**
     * 回写结果：排队中 -> 成功 / 失败。
     * 返回 0 行说明异步时序下这行还没插入（极少见），调用方会补一条完整流水。
     */
    @Update("UPDATE t_seckill_flow SET status = #{status}, fail_reason = #{failReason}," +
            " cost_ms = #{costMs}, finish_time = NOW()" +
            " WHERE order_no = #{orderNo}")
    int updateResult(@Param("orderNo") String orderNo, @Param("status") Integer status,
                     @Param("failReason") String failReason, @Param("costMs") Long costMs);

    @Select("SELECT id, batch_no, order_no, user_id, train_id, seat_type, passenger_name, ticket_index," +
            " status, fail_reason, queue_seq, cost_ms, client_ip, create_time, finish_time" +
            " FROM t_seckill_flow WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit}")
    List<SeckillFlowDO> selectByUser(@Param("userId") Long userId, @Param("limit") Long limit);

    @Select("SELECT id, batch_no, order_no, user_id, train_id, seat_type, passenger_name, ticket_index," +
            " status, fail_reason, queue_seq, cost_ms, client_ip, create_time, finish_time" +
            " FROM t_seckill_flow WHERE batch_no = #{batchNo} ORDER BY id")
    List<SeckillFlowDO> selectByBatch(@Param("batchNo") String batchNo);

    @Select("<script>SELECT id, batch_no, order_no, user_id, train_id, seat_type, passenger_name, ticket_index," +
            " status, fail_reason, queue_seq, cost_ms, client_ip, create_time, finish_time" +
            " FROM t_seckill_flow" +
            "<where>" +
            "<if test=\"userId != null\">AND user_id = #{userId}</if>" +
            "<if test=\"trainId != null\">AND train_id = #{trainId}</if>" +
            "<if test=\"status != null\">AND status = #{status}</if>" +
            "</where> ORDER BY id DESC LIMIT #{offset}, #{limit}</script>")
    List<SeckillFlowDO> selectPage(@Param("userId") Long userId, @Param("trainId") Long trainId,
                                   @Param("status") Integer status,
                                   @Param("offset") Long offset, @Param("limit") Long limit);

    @Select("<script>SELECT COUNT(1) FROM t_seckill_flow" +
            "<where>" +
            "<if test=\"userId != null\">AND user_id = #{userId}</if>" +
            "<if test=\"trainId != null\">AND train_id = #{trainId}</if>" +
            "<if test=\"status != null\">AND status = #{status}</if>" +
            "</where></script>")
    long countPage(@Param("userId") Long userId, @Param("trainId") Long trainId, @Param("status") Integer status);
}

package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.TrainSegmentStockDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 区间（相邻站单段）库存 Mapper。
 */
@Repository
public interface TrainSegmentStockMapper {

    @Select("SELECT id, train_id, seat_type, seg_index, total_count, available_count, version, create_time, update_time" +
            " FROM t_train_segment_stock WHERE train_id = #{trainId} ORDER BY seat_type, seg_index")
    List<TrainSegmentStockDO> selectByTrainId(@Param("trainId") Long trainId);

    @Select("SELECT id, train_id, seat_type, seg_index, total_count, available_count, version, create_time, update_time" +
            " FROM t_train_segment_stock WHERE train_id = #{trainId} AND seat_type = #{seatType}" +
            " AND seg_index >= #{fromOrder} AND seg_index < #{toOrder} ORDER BY seg_index")
    List<TrainSegmentStockDO> selectRange(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                                          @Param("fromOrder") Integer fromOrder, @Param("toOrder") Integer toOrder);

    /**
     * 区间余票 = 覆盖各段余票的最小值；没有段数据返回 null（调用方按全程票兜底）。
     */
    @Select("SELECT MIN(available_count) FROM t_train_segment_stock" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType}" +
            " AND seg_index >= #{fromOrder} AND seg_index < #{toOrder}")
    Integer minAvailable(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                         @Param("fromOrder") Integer fromOrder, @Param("toOrder") Integer toOrder);

    /**
     * 占用区间：覆盖的每一段都扣 count 张，available_count 不足的段不会被更新（行级条件保证不超卖）。
     *
     * @return 实际更新的段数，调用方需与期望段数比对，不一致则回滚事务
     */
    @Update("UPDATE t_train_segment_stock SET available_count = available_count - #{count}, version = version + 1" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType}" +
            " AND seg_index >= #{fromOrder} AND seg_index < #{toOrder} AND available_count >= #{count}")
    int decreaseRange(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                      @Param("fromOrder") Integer fromOrder, @Param("toOrder") Integer toOrder,
                      @Param("count") int count);

    /**
     * 释放区间：覆盖的每一段都归还 count 张，并且不超过 total_count（防止重复释放把库存刷大）。
     */
    @Update("UPDATE t_train_segment_stock SET available_count = LEAST(available_count + #{count}, total_count)," +
            " version = version + 1" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType}" +
            " AND seg_index >= #{fromOrder} AND seg_index < #{toOrder}")
    int increaseRange(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                      @Param("fromOrder") Integer fromOrder, @Param("toOrder") Integer toOrder,
                      @Param("count") int count);

    @Insert("INSERT INTO t_train_segment_stock (train_id, seat_type, seg_index, total_count, available_count," +
            " version, create_time, update_time)" +
            " VALUES (#{trainId}, #{seatType}, #{segIndex}, #{totalCount}, #{availableCount}, 0, NOW(), NOW())" +
            " ON DUPLICATE KEY UPDATE total_count = VALUES(total_count), update_time = NOW()")
    int upsert(TrainSegmentStockDO stock);

    @Select("SELECT COUNT(1) FROM t_train_segment_stock WHERE train_id = #{trainId}")
    int countByTrainId(@Param("trainId") Long trainId);
}

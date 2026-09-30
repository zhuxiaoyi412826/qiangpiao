package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.TrainStockDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车次席别库存 Mapper（秒杀库存，乐观锁防超卖）。
 */
@Repository
public interface TrainStockMapper {

    TrainStockDO selectById(@Param("id") Long id);

    TrainStockDO selectByTrainAndType(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    List<TrainStockDO> selectByTrainId(@Param("trainId") Long trainId);

    /**
     * 乐观锁扣减库存：available_count > 0 时扣减，返回影响行数
     */
    int decreaseStock(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 按车次 + 席别原子扣减库存：available_count > 0 时扣减，返回影响行数。
     * 相比 decreaseStock 少了 select + version 的重试，批量并发下不会因重试耗尽而误判售罄。
     */
    int decreaseStockByTrain(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    /**
     * 回滚库存（下单失败时补偿）
     */
    int increaseStock(@Param("id") Long id);

    int updateById(TrainStockDO stock);

    int insert(TrainStockDO stock);
}

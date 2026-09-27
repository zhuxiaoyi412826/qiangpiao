package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.SeckillRecordDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 秒杀记录 Mapper（一人一单幂等）。
 */
@Repository
public interface SeckillRecordMapper {

    /**
     * 插入成功记录，唯一键冲突时返回 0（表示重复抢票）
     */
    int insertIgnore(SeckillRecordDO record);

    SeckillRecordDO selectByUserAndTrain(@Param("trainId") Long trainId,
                                         @Param("seatType") Integer seatType,
                                         @Param("userId") Long userId);

    int deleteByOrderNo(@Param("orderNo") String orderNo);
}

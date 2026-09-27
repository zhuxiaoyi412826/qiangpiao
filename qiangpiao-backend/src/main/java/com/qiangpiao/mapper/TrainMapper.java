package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.TrainDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 车次 Mapper。
 */
@Repository
public interface TrainMapper {

    TrainDO selectById(@Param("id") Long id);

    List<TrainDO> selectByRoute(@Param("fromStation") String fromStation,
                                @Param("toStation") String toStation,
                                @Param("departDate") LocalDate departDate,
                                @Param("offset") Long offset,
                                @Param("limit") Long limit);

    long countByRoute(@Param("fromStation") String fromStation,
                      @Param("toStation") String toStation,
                      @Param("departDate") LocalDate departDate);

    List<TrainDO> selectAll(@Param("offset") Long offset, @Param("limit") Long limit);

    long countAll();

    int insert(TrainDO train);

    /**
     * 车次日期滚动：把发车日期早于指定日期的车次统一改到该日期（保证每天都有车次可查）。
     *
     * @return 影响行数
     */
    int rollExpiredTrains(@Param("today") LocalDate today);
}

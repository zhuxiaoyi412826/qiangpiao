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

    /**
     * 区间票查询：按「经停站」匹配，支持中途上车 / 中途下车
     * （from 站必须在 to 站之前，靠 t_train_stop.stop_order 保证方向正确）。
     */
    List<TrainDO> selectBySegment(@Param("fromStation") String fromStation,
                                  @Param("toStation") String toStation,
                                  @Param("departDate") LocalDate departDate,
                                  @Param("offset") Long offset,
                                  @Param("limit") Long limit);

    long countBySegment(@Param("fromStation") String fromStation,
                        @Param("toStation") String toStation,
                        @Param("departDate") LocalDate departDate);

    /**
     * 按车次号查询（前缀匹配：G180 能查出 G180、G1801 等）：
     * departDate 为空时返回该车次全部未发车班次，按发车日期升序。
     */
    List<TrainDO> selectByTrainNo(@Param("trainNo") String trainNo,
                                  @Param("departDate") LocalDate departDate,
                                  @Param("offset") Long offset,
                                  @Param("limit") Long limit);

    long countByTrainNo(@Param("trainNo") String trainNo,
                        @Param("departDate") LocalDate departDate);

    List<TrainDO> selectAll(@Param("offset") Long offset, @Param("limit") Long limit);

    long countAll();

    int insert(TrainDO train);

    /**
     * 车次日期滚动：把发车日期早于指定日期的车次统一改到该日期（保证每天都有车次可查）。
     *
     * @return 影响行数
     * @deprecated 已改用「按日期生成班次」{@link #selectTemplates()} + 复制库存座位，
     * 日期滚动会让历史订单的车次日期被改写，故不再调度
     */
    int rollExpiredTrains(@Param("today") LocalDate today);

    /**
     * 车次模板：每个车次号取发车日期最早的一条，作为生成每日班次的模板（只取在售车次）。
     */
    List<TrainDO> selectTemplates();

    /**
     * 查询某车次在某发车日期的班次 id（生成班次前做幂等判断）。
     */
    Long selectIdByNoAndDate(@Param("trainNo") String trainNo, @Param("departDate") LocalDate departDate);

    /**
     * 车次时刻表：按停靠顺序返回途经站及到发时刻。
     */
    List<com.qiangpiao.dataobject.TrainStopDO> listStops(@Param("trainId") Long trainId);
}

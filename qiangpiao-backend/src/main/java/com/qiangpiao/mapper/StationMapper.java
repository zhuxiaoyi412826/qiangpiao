package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.StationDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车站 Mapper。
 */
@Repository
public interface StationMapper {

    List<StationDO> selectAll();

    StationDO selectById(@Param("id") Long id);

    StationDO selectByName(@Param("stationName") String stationName);

    List<StationDO> selectByKeyword(@Param("keyword") String keyword);
}

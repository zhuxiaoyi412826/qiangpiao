package com.qiangpiao.service.impl;

import com.qiangpiao.cache.MultiLevelCacheService;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.dataobject.StationDO;
import com.qiangpiao.mapper.StationMapper;
import com.qiangpiao.service.StationService;
import com.qiangpiao.vo.StationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车站服务实现（L1 -> L2 -> L3 三级缓存）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {

    private final StationMapper stationMapper;
    private final MultiLevelCacheService cacheService;

    @Value("${cache.default-ttl}")
    private long cacheTtl;

    @Override
    public List<StationVO> listAll() {
        List<StationVO> stations = cacheService.getList(Constants.CACHE_STATION, StationVO.class, () ->
                stationMapper.selectAll().stream().map(this::toVO).collect(Collectors.toList()), cacheTtl);
        return stations == null ? Collections.emptyList() : stations;
    }

    @Override
    public List<StationVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return listAll();
        }
        return stationMapper.selectByKeyword(keyword.trim()).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private StationVO toVO(StationDO station) {
        return StationVO.builder()
                .id(station.getId())
                .stationName(station.getStationName())
                .city(station.getCity())
                .pyCode(station.getPyCode())
                .build();
    }
}

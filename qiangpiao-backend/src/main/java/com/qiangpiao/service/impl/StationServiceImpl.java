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

    /**
     * 车站搜索：城市名 / 拼音简码 / 站名。
     * <ul>
     *   <li>输入的是完整站名 → 只返回这一个车站（避免同城的其它站混进来）；</li>
     *   <li>输入的是城市名或拼音简码 → 返回该城市下的全部车站；</li>
     *   <li>否则按站名 / 城市 / 拼音简码模糊匹配。</li>
     * </ul>
     */
    @Override
    public List<StationVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return listAll();
        }
        String kw = keyword.trim();
        // 1. 具体车站优先：命中就只出这一个，用户不用在同城列表里再挑一次
        StationDO exact = stationMapper.selectByName(kw);
        if (exact != null) {
            return Collections.singletonList(toVO(exact));
        }
        // 2. 城市 / 拼音简码 / 站名模糊：命中城市时会带出该城市全部车站
        return stationMapper.selectByKeyword(kw).stream()
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

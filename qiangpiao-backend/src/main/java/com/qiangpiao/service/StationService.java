package com.qiangpiao.service;

import com.qiangpiao.vo.StationVO;

import java.util.List;

/**
 * 车站服务（热点数据，走三级缓存）。
 */
public interface StationService {

    /**
     * 全部车站（高频读：L1 -> L2 -> L3）
     */
    List<StationVO> listAll();

    /**
     * 关键字搜索
     */
    List<StationVO> search(String keyword);
}

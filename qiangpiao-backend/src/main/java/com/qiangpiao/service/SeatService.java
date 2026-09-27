package com.qiangpiao.service;

import com.qiangpiao.bo.SeatBO;
import com.qiangpiao.vo.SeatMapVO;

/**
 * 座位服务：座位图查询（三级缓存）与座位抢占 / 释放。
 */
public interface SeatService {

    /**
     * 座位图
     */
    SeatMapVO seatMap(Long trainId, Integer seatType);

    /**
     * 抢占座位（乐观锁，并发安全）
     *
     * @param trainId  车次ID
     * @param seatType 席别
     * @param seatId   指定座位ID，可为空则自动分配
     * @param orderNo  订单号
     */
    SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo);

    /**
     * 释放座位（取消订单 / 下单失败补偿）
     */
    void releaseSeat(String orderNo);

    /**
     * 失效座位缓存
     */
    void evictSeatCache(Long trainId, Integer seatType);
}

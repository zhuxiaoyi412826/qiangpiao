package com.qiangpiao.service;

import com.qiangpiao.bo.RangeBO;
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
     * 按乘车区间取座位图：只有「本区间已被占用」的座位不可选，
     * 该座位其它区间被占用不影响本次购买（区间复用）。
     */
    SeatMapVO seatMap(Long trainId, Integer seatType, RangeBO range);

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
     * 抢占座位（并发安全，支持「优先同车厢」）
     *
     * @param seatId           指定座位ID，为空则自动分配
     * @param preferCarriageNo 优先分配的车厢号（批量购票让同行人坐一起），为空 / 该车厢无座时自动改分配其它车厢
     */
    SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo, Integer preferCarriageNo);

    /**
     * 抢占座位（区间票）
     *
     * @param range 乘车区间；为空按全程票处理
     */
    SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo,
                           Integer preferCarriageNo, RangeBO range);

    /**
     * 释放座位（取消订单 / 下单失败补偿）
     */
    void releaseSeat(String orderNo);

    /**
     * 失效座位缓存
     */
    void evictSeatCache(Long trainId, Integer seatType);
}

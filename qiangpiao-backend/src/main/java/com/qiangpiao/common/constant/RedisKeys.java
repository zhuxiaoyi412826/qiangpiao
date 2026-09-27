package com.qiangpiao.common.constant;

/**
 * Redis key 构建工具：统一 key 规范，避免各服务散落拼串。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static String trainList(String from, String to, String date) {
        return Constants.CACHE_TRAIN_LIST + from + "-" + to + "-" + date;
    }

    public static String trainDetail(Long trainId) {
        return Constants.CACHE_TRAIN_DETAIL + trainId;
    }

    public static String seatMap(Long trainId, Integer seatType) {
        return Constants.CACHE_SEAT_MAP + trainId + ":" + seatType;
    }

    public static String orderDetail(String orderNo) {
        return Constants.CACHE_ORDER_DETAIL + orderNo;
    }

    /** 秒杀库存 key：trainId + seatType */
    public static String seckillStock(Long trainId, Integer seatType) {
        return Constants.STOCK_KEY + trainId + ":" + seatType;
    }

    /** 一人一单 key */
    public static String seckillUser(Long trainId, Integer seatType, Long userId) {
        return Constants.SECKILL_USER_KEY + trainId + ":" + seatType + ":" + userId;
    }

    /** 抢票结果 key（订单号） */
    public static String seckillResult(Long trainId, Integer seatType, Long userId) {
        return Constants.SECKILL_RESULT_KEY + trainId + ":" + seatType + ":" + userId;
    }

    /** 限流 key */
    public static String userLimit(Long userId) {
        return Constants.LIMIT_KEY + userId;
    }

    /** 分布式锁 key */
    public static String lock(String bizKey) {
        return Constants.LOCK_KEY + bizKey;
    }
}

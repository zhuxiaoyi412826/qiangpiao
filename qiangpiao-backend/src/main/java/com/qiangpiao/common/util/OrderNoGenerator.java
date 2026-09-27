package com.qiangpiao.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成：时间戳 + 用户尾号 + 随机数，保证可读性且不依赖发号器。
 */
public final class OrderNoGenerator {

    private OrderNoGenerator() {
    }

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");

    public static String generate(Long userId) {
        String time = SDF.format(new Date());
        String userTail = String.format("%04d", (userId == null ? 0 : userId) % 10000);
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "QP" + time + userTail + rand;
    }
}

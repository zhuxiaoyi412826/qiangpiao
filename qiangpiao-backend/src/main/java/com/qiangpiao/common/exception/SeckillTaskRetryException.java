package com.qiangpiao.common.exception;

/**
 * 秒杀下单任务「可重试」异常。
 * <p>
 * 与 {@link BizException} 的区别：BizException 表示业务上注定失败（限购 / 库存不足 / 重复下单），
 * 重试一万次也是失败，队列应立即补偿并确认（XACK）；
 * 本异常表示系统抖动（DB 超时、连接池打满、Redis 瞬时不可用等），
 * 队列不确认消息，让它留在 pending 里等待重新投递。
 */
public class SeckillTaskRetryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SeckillTaskRetryException(String message) {
        super(message);
    }

    public SeckillTaskRetryException(Throwable cause) {
        super(cause);
    }
}

package com.qiangpiao.service;

import com.qiangpiao.bo.OrderBO;
import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dto.OrderQueryDTO;
import com.qiangpiao.dto.PayDTO;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.vo.OrderDetailVO;
import com.qiangpiao.vo.OrderVO;
import com.qiangpiao.vo.PaymentVO;

/**
 * 订单服务。
 */
public interface OrderService {

    /**
     * 我的订单（分页）
     */
    PageResult<OrderVO> page(Long userId, OrderQueryDTO queryDTO);

    /**
     * 订单详情
     */
    OrderDetailVO detail(String orderNo, Long userId);

    /**
     * 发起支付（第一阶段）：创建支付单，等待渠道异步回调后才真正扣款。
     *
     * @return 支付单（前端据此轮询支付结果）
     */
    com.qiangpiao.vo.PaymentVO pay(com.qiangpiao.dto.PayDTO payDTO, Long userId);

    /**
     * 取消订单：释放座位 + 回滚库存
     */
    void cancel(String orderNo, Long userId);

    /**
     * 订单业务对象（Service 之间调用）
     */
    OrderBO getOrderBO(String orderNo);

    /**
     * 秒杀异步落库：幂等 + 乐观锁扣库存 + 占座 + 建单（事务）
     */
    OrderDO createSeckillOrder(SeckillTaskBO taskBO);

    /**
     * 关闭超时未支付订单（定时任务）
     */
    int closeExpiredOrders();
}

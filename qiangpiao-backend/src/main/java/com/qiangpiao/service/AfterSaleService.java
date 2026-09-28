package com.qiangpiao.service;

import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderLogDO;

import java.util.List;

/**
 * 售后服务：退票 / 改签 / 售后记录（时间轴与改签历史）。
 * 用户自助与后台客服人工处理复用同一套逻辑，区别在于 operator 与是否校验归属。
 */
public interface AfterSaleService {

    /**
     * 退票：释放座位、归还库存、票款退回用户钱包、平台账户冲退，并写流转日志。
     *
     * @param userId   前台自助退票时传用户ID做归属校验；后台人工退票可传 null 跳过归属校验
     * @param operator 操作人标记（用户ID 或 admin）
     */
    void refund(String orderNo, Long userId, String operator, String reason);

    /**
     * 改签：换到新车次同席别，差额多退少补，写改签记录与流转日志。
     */
    void change(String orderNo, Long userId, Long newTrainId, Integer seatType, String reason);

    /** 订单时间轴（物流式节点） */
    List<OrderLogDO> timeline(String orderNo);

    /** 改签历史 */
    List<OrderChangeDO> changes(String orderNo);
}

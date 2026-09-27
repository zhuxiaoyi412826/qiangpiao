package com.qiangpiao.service;

import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dto.TrainQueryDTO;
import com.qiangpiao.vo.TrainDetailVO;
import com.qiangpiao.vo.TrainVO;

/**
 * 车次服务。
 */
public interface TrainService {

    /**
     * 分页查询车次（三级缓存）
     */
    PageResult<TrainVO> query(TrainQueryDTO queryDTO);

    /**
     * 车次详情（三级缓存 + 座位图）
     */
    TrainDetailVO detail(Long trainId);

    /**
     * 车次业务对象（供其他 Service 调用，不经 Controller）
     */
    TrainBO getTrainBO(Long trainId);

    /**
     * 失效车次缓存（下单 / 取消后调用，保证一致性）
     */
    void evictTrainCache(Long trainId);

    /**
     * 车次日期滚动：把发车日期早于今天的车次统一改到今天（每天发车），并清理车次缓存。
     *
     * @return 更新的车次数
     */
    int rollExpiredTrains();

    /**
     * 校验车次是否可购票（未发车 + 预售期内 + 未到停售时间），不通过直接抛业务异常。
     * 售票规则集中在车次服务里，秒杀链路复用同一套判定。
     */
    void assertTicketSellable(Long trainId);
}

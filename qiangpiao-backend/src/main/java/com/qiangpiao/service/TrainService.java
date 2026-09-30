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
     * 车次详情（按乘车区间）：余票与座位图都按「上车站 → 下车站」计算，
     * 席位只在当前区间内被占用时才不可选（区间复用）。
     *
     * @param fromStation 上车站名，为空表示始发站
     * @param toStation   下车站名，为空表示终点站
     */
    TrainDetailVO detail(Long trainId, String fromStation, String toStation);

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

    /**
     * 购票资格预检：返回不能购买的原因（限购 / 行程运行时间冲突），null 表示可以购买。
     * 供前端在点击抢票前提示用户，避免无意义的请求。
     */
    String buyBlockReason(Long userId, Long trainId);

    /**
     * 车次时刻表（站点时序）：按停靠顺序返回途经站及到发时刻。
     */
    java.util.List<com.qiangpiao.dataobject.TrainStopDO> stops(Long trainId);
}

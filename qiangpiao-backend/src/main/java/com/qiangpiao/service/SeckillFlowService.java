package com.qiangpiao.service;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dataobject.SeckillFlowDO;
import com.qiangpiao.vo.SeckillFlowVO;

import java.util.List;

/**
 * 抢票流水服务：一次抢票请求从受理到出结果的完整留痕（异步旁路写入，失败不影响抢票）。
 */
public interface SeckillFlowService {

    /** 受理：记一条排队中的流水（异步写入，不拖慢抢票响应） */
    void accept(SeckillFlowDO flow);

    /** 出结果：成功 / 失败回写状态、耗时与失败原因 */
    void finish(String orderNo, int status, String failReason, Long costMs);

    /** 我的抢票记录 */
    List<SeckillFlowVO> myFlows(Long userId, int limit);

    /** 按批次查流水（一次买多张时看这一批每张的结果） */
    List<SeckillFlowVO> batchFlows(String batchNo);

    /** 后台：抢票流水分页 */
    PageResult<SeckillFlowVO> page(Long userId, Long trainId, Integer status, Integer pageNum, Integer pageSize);

    List<SeckillFlowDO> listByBatch(String batchNo);
}

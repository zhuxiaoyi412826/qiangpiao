package com.qiangpiao.service;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dto.TicketQueryDTO;
import com.qiangpiao.vo.TicketVO;

/**
 * 车票服务：查询「未开车」与「历史」两类车票。
 */
public interface TicketService {

    /**
     * 我的车票（分页）。
     *
     * @param userId   当前登录用户
     * @param queryDTO type=upcoming 未开车；type=history 历史车票
     */
    PageResult<TicketVO> pageTickets(Long userId, TicketQueryDTO queryDTO);
}

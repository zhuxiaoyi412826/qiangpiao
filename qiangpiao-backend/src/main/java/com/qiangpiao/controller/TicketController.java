package com.qiangpiao.controller;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.dto.TicketQueryDTO;
import com.qiangpiao.service.TicketService;
import com.qiangpiao.vo.TicketVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 我的车票接口：未开车（待出行）/ 历史车票。
 */
@RestController
@RequestMapping("/api/tickets")
@Api(tags = "我的车票")
@Validated
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @ApiOperation("我的车票（type=upcoming 未开车 / history 历史）")
    public R<PageResult<TicketVO>> page(@Valid TicketQueryDTO queryDTO) {
        return R.ok(ticketService.pageTickets(SecurityUtils.currentUserId(), queryDTO));
    }
}

package com.qiangpiao.controller;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.dto.OrderQueryDTO;
import com.qiangpiao.dto.PayDTO;
import com.qiangpiao.service.OrderService;
import com.qiangpiao.vo.OrderDetailVO;
import com.qiangpiao.vo.OrderVO;
import com.qiangpiao.vo.PaymentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 订单接口（RESTful）。
 */
@RestController
@RequestMapping("/api/orders")
@Api(tags = "订单")
@Validated
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @ApiOperation("我的订单列表")
    public R<PageResult<OrderVO>> list(@Valid OrderQueryDTO queryDTO) {
        return R.ok(orderService.page(SecurityUtils.currentUserId(), queryDTO));
    }

    @GetMapping("/{orderNo}")
    @ApiOperation("订单详情")
    public R<OrderDetailVO> detail(@PathVariable String orderNo) {
        return R.ok(orderService.detail(orderNo, SecurityUtils.currentUserId()));
    }

    @PostMapping("/pay")
    @ApiOperation("发起支付（第一阶段）：创建支付单，等待渠道异步回调后扣款")
    public R<PaymentVO> pay(@Valid @RequestBody PayDTO payDTO) {
        return R.ok(orderService.pay(payDTO, SecurityUtils.currentUserId()));
    }

    @PostMapping("/{orderNo}/cancel")
    @ApiOperation("取消订单（释放座位并归还库存）")
    public R<Void> cancel(@PathVariable String orderNo) {
        orderService.cancel(orderNo, SecurityUtils.currentUserId());
        return R.ok();
    }
}

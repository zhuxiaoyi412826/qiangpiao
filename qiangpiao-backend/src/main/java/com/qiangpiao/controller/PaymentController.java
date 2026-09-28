package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.service.PaymentService;
import com.qiangpiao.vo.PaymentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付接口：查询支付单 + 渠道回调（回调无需登录，靠验签保证来源可信）。
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@Api(tags = "支付")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{payNo}")
    @ApiOperation("查询支付单状态（发起支付后轮询）")
    public R<PaymentVO> detail(@PathVariable String payNo) {
        PaymentVO payment = paymentService.getByPayNo(payNo);
        Long userId = SecurityUtils.currentUserId();
        if (userId != null && payment.getUserId() != null && !userId.equals(payment.getUserId())) {
            // 非本人支付单不暴露
            throw new com.qiangpiao.common.exception.BizException(
                    com.qiangpiao.common.result.ResultCode.FORBIDDEN);
        }
        return R.ok(payment);
    }

    /**
     * 渠道异步回调：验签 + 时间戳窗口 + nonce 防重放 + 支付单幂等。
     * 生产环境由支付宝 / 微信服务器调用，此处为模拟渠道。
     */
    @PostMapping("/notify")
    @ApiOperation("支付结果回调（渠道调用，验签 + 幂等）")
    public R<Boolean> notify(@RequestBody Map<String, Object> params) {
        boolean accepted = paymentService.handleNotify(params);
        return R.ok(accepted);
    }

    /**
     * 模拟渠道回调：代替第三方渠道主动推送结果，便于本地演示两阶段支付。
     */
    @PostMapping("/{payNo}/mock-callback")
    @ApiOperation("模拟渠道回调（演示用：SUCCESS / FAIL）")
    public R<Boolean> mockCallback(@PathVariable String payNo,
                                   @RequestParam(required = false, defaultValue = "SUCCESS") String result) {
        return R.ok(paymentService.mockCallback(payNo, result));
    }
}

package com.qiangpiao.controller;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.dto.RechargeDTO;
import com.qiangpiao.service.WalletService;
import com.qiangpiao.vo.WalletFlowVO;
import com.qiangpiao.vo.WalletVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 钱包接口：余额 / 零钱流水 / 自定义充值。
 */
@RestController
@RequestMapping("/api/wallet")
@Api(tags = "钱包")
@Validated
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @ApiOperation("钱包余额（累计充值/消费）")
    public R<WalletVO> info() {
        return R.ok(walletService.getWallet(SecurityUtils.currentUserId()));
    }

    @GetMapping("/flows")
    @ApiOperation("零钱流水（分页）")
    public R<PageResult<WalletFlowVO>> flows(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(walletService.pageFlows(SecurityUtils.currentUserId(), pageNum, pageSize));
    }

    @PostMapping("/recharge")
    @ApiOperation("自定义金额充值（模拟）")
    public R<WalletVO> recharge(@Valid @RequestBody RechargeDTO dto) {
        return R.ok(walletService.recharge(SecurityUtils.currentUserId(), dto));
    }
}

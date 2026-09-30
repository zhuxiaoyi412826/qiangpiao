package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.dto.ChangeDTO;
import com.qiangpiao.dto.RefundDTO;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.service.AfterSaleService;
import com.qiangpiao.vo.AfterSalePreviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 退票 / 改签 / 售后记录（用户自助）。
 */
@RestController
@RequestMapping("/api/after-sale")
@Api(tags = "退票/改签")
@RequiredArgsConstructor
public class RefundController {

    private final AfterSaleService afterSaleService;

    @PostMapping("/refund")
    @ApiOperation("退票：已支付且未发车，票款退回钱包")
    public R<Boolean> refund(@RequestBody RefundDTO dto) {
        Long userId = SecurityUtils.currentUserId();
        afterSaleService.refund(dto.getOrderNo(), userId, String.valueOf(userId), dto.getReason());
        return R.ok(true);
    }

    @PostMapping("/change")
    @ApiOperation("改签：换到新车次，差额多退少补")
    public R<Boolean> change(@RequestBody ChangeDTO dto) {
        Long userId = SecurityUtils.currentUserId();
        afterSaleService.change(dto.getOrderNo(), userId, dto.getNewTrainId(), dto.getSeatType(), dto.getReason());
        return R.ok(true);
    }

    @GetMapping("/{orderNo}/refund-preview")
    @ApiOperation("退票费用试算：票价 / 手续费 / 实退金额 / 计费档位")
    public R<AfterSalePreviewVO> refundPreview(@PathVariable String orderNo) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(afterSaleService.previewRefund(orderNo, userId));
    }

    @GetMapping("/{orderNo}/change-preview")
    @ApiOperation("改签费用试算：差额 / 手续费 / 实退或实补")
    public R<AfterSalePreviewVO> changePreview(@PathVariable String orderNo,
                                               @RequestParam Long newTrainId,
                                               @RequestParam Integer seatType) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(afterSaleService.previewChange(orderNo, userId, newTrainId, seatType));
    }

    @GetMapping("/{orderNo}/timeline")
    @ApiOperation("订单时间轴（物流式节点）")
    public R<List<OrderLogDO>> timeline(@PathVariable String orderNo) {
        return R.ok(afterSaleService.timeline(orderNo));
    }

    @GetMapping("/{orderNo}/changes")
    @ApiOperation("改签历史")
    public R<List<OrderChangeDO>> changes(@PathVariable String orderNo) {
        return R.ok(afterSaleService.changes(orderNo));
    }
}

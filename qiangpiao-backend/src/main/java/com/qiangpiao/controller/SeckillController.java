package com.qiangpiao.controller;

import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.IpUtils;
import com.qiangpiao.common.util.JwtTokenUtil;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dto.SeckillDTO;
import com.qiangpiao.service.SeckillFlowService;
import com.qiangpiao.service.SeckillService;
import com.qiangpiao.service.SeckillSseService;
import com.qiangpiao.vo.SeckillBatchResultVO;
import com.qiangpiao.vo.SeckillFlowVO;
import com.qiangpiao.vo.SeckillResultVO;
import com.qiangpiao.vo.SeckillStatusVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 秒杀抢票接口。
 */
@RestController
@RequestMapping("/api/seckill")
@Api(tags = "秒杀抢票")
@Validated
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final SeckillFlowService seckillFlowService;
    private final SeckillSseService seckillSseService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/do")
    @ApiOperation("抢票（异步下单，返回排队中）")
    public R<SeckillResultVO> seckill(@Valid @RequestBody SeckillDTO seckillDTO, HttpServletRequest request) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(seckillService.seckill(userId, seckillDTO, IpUtils.getIp(request)));
    }

    /**
     * 抢票结果推送（SSE）。
     * EventSource 不支持自定义请求头，因此 token 走 query 参数；
     * 浏览器不支持或连接断开时，前端自动降级为轮询兜底。
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    @ApiOperation("订阅抢票结果推送（SSE，轮询作为兜底）")
    public SseEmitter stream(@RequestParam String batchNo,
                             @RequestParam(required = false) String token,
                             @RequestParam(required = false) String traceId) {
        // EventSource 带不了请求头，traceId 走 query：
        // 与抢票请求共用同一个 ID，订阅 / 推送 / 下单三段日志可串成一条链路
        if (StringUtils.hasText(traceId)) {
            TraceContext.putTraceId(traceId.trim());
        }
        Long userId = null;
        try {
            userId = SecurityUtils.currentUserId();
        } catch (Exception ignored) {
            // 未走 Spring Security 上下文时，从 token 参数解析
        }
        if (userId == null && StringUtils.hasText(token)) {
            userId = jwtTokenUtil.getUserId(token);
        }
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return seckillSseService.subscribe(userId, batchNo);
    }

    @GetMapping("/result")
    @ApiOperation("轮询抢票结果")
    public R<SeckillStatusVO> result(@RequestParam Long trainId, @RequestParam Integer seatType) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(seckillService.queryResult(trainId, seatType, userId));
    }

    @GetMapping("/batch/result")
    @ApiOperation("轮询批量抢票结果（一次买多张，逐张给结果）")
    public R<SeckillBatchResultVO> batchResult(@RequestParam String batchNo) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(seckillService.queryBatchResult(userId, batchNo));
    }

    @GetMapping("/flows")
    @ApiOperation("我的抢票记录（抢票流水：受理时间 / 结果 / 耗时 / 失败原因）")
    public R<List<SeckillFlowVO>> flows(@RequestParam(required = false, defaultValue = "20") Integer limit) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(seckillFlowService.myFlows(userId, limit == null ? 20 : limit));
    }

    @GetMapping("/batch/flows")
    @ApiOperation("按批次查抢票流水（一次买多张时看这一批每张的结果）")
    public R<List<SeckillFlowVO>> batchFlows(@RequestParam String batchNo) {
        return R.ok(seckillFlowService.batchFlows(batchNo));
    }

    @GetMapping("/stock")
    @ApiOperation("查询实时余票（Redis 库存）")
    public R<Integer> stock(@RequestParam Long trainId, @RequestParam Integer seatType) {
        return R.ok(seckillService.availableStock(trainId, seatType));
    }

    @PostMapping("/preheat/{trainId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiOperation("预热指定车次的秒杀库存（管理员）")
    public R<Void> preheat(@PathVariable Long trainId) {
        seckillService.preheatStock(trainId);
        return R.ok();
    }

    @PostMapping("/preheat")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiOperation("全量预热秒杀库存（管理员）")
    public R<Void> preheatAll() {
        seckillService.preheatAllStock();
        return R.ok();
    }
}

package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查（探活 / 网关校验）。
 */
@RestController
@RequestMapping("/api/health")
@Api(tags = "健康检查")
public class HealthController {

    @GetMapping
    @ApiOperation("健康检查")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>(4);
        data.put("status", "UP");
        data.put("time", LocalDateTime.now().toString());
        return R.ok(data);
    }
}

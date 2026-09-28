package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.dataobject.AnnouncementDO;
import com.qiangpiao.service.AdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告（前台展示，免登录）。
 */
@RestController
@RequestMapping("/api/announcements")
@Api(tags = "公告")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AdminService adminService;

    @GetMapping
    @ApiOperation("已发布公告")
    public R<List<AnnouncementDO>> list(@RequestParam(defaultValue = "5") Integer limit) {
        return R.ok(adminService.activeAnnouncements(limit));
    }
}

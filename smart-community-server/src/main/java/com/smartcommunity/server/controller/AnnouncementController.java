package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Announcement;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "公告管理", description = "公告增删改查")
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Operation(summary = "分页查询公告")
    @RequirePermission("property:announcement:list")
    @GetMapping("/page")
    public Result<PageResult<Announcement>> page(PageQuery pageQuery,
                                                  @RequestParam(required = false) Long communityId,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) Integer expired,
                                                  @RequestParam(required = false) Integer top) {
        return Result.success(announcementService.pageList(pageQuery, communityId, status, expired, top));
    }

    @Operation(summary = "获取公告详情")
    @RequirePermission("property:announcement:list")
    @GetMapping("/{id}")
    public Result<Announcement> getById(@PathVariable Long id) {
        return Result.success(announcementService.getById(id));
    }

    @Operation(summary = "首页公告列表")
    @GetMapping("/home")
    public Result<List<Announcement>> homeList() {
        return Result.success(announcementService.getHomeAnnouncements());
    }

    @Operation(summary = "新增公告")
    @RequirePermission("property:announcement:list")
    @OperateLog(module = "公告管理", operation = "新增")
    @PostMapping
    public Result<Void> add(@RequestBody Announcement announcement) {
        announcementService.add(announcement);
        return Result.success();
    }

    @Operation(summary = "更新公告")
    @RequirePermission("property:announcement:list")
    @OperateLog(module = "公告管理", operation = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Announcement announcement) {
        announcementService.update(announcement);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @RequirePermission("property:announcement:list")
    @OperateLog(module = "公告管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }
}

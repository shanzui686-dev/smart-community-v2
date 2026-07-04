package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Visitor;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "访客管理", description = "访客登记与进出管理")
@RestController
@RequestMapping("/api/access/visitor")
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Operation(summary = "分页查询访客")
    @RequirePermission("access:visitor:list")
    @GetMapping("/list")
    public Result<PageResult<Visitor>> list(PageQuery pageQuery,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) Long communityId,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(visitorService.pageList(pageQuery, name, communityId, status));
    }

    @Operation(summary = "获取访客详情")
    @RequirePermission("access:visitor:list")
    @GetMapping("/{id}")
    public Result<Visitor> getById(@PathVariable Long id) {
        return Result.success(visitorService.getById(id));
    }

    @OperateLog(module = "访客登记", operation = "新增")
    @Operation(summary = "新增访客登记")
    @RequirePermission("access:visitor:list")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Visitor visitor) {
        visitorService.add(visitor);
        return Result.success();
    }

    @OperateLog(module = "访客登记", operation = "修改")
    @Operation(summary = "修改访客信息")
    @RequirePermission("access:visitor:list")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Visitor visitor) {
        visitorService.update(visitor);
        return Result.success();
    }

    @OperateLog(module = "访客登记", operation = "取消")
    @Operation(summary = "取消预约")
    @RequirePermission("access:visitor:list")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        visitorService.cancel(id);
        return Result.success();
    }

    @OperateLog(module = "访客登记", operation = "签到")
    @Operation(summary = "访客签到")
    @RequirePermission("access:visitor:list")
    @PutMapping("/{id}/check-in")
    public Result<Void> checkIn(@PathVariable Long id) {
        visitorService.checkIn(id);
        return Result.success();
    }

    @OperateLog(module = "访客登记", operation = "签退")
    @Operation(summary = "访客签退")
    @RequirePermission("access:visitor:list")
    @PutMapping("/{id}/check-out")
    public Result<Void> checkOut(@PathVariable Long id) {
        visitorService.checkOut(id);
        return Result.success();
    }
}

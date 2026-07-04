package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小区管理", description = "小区信息增删改查")
@RestController
@RequestMapping("/api/property/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @Operation(summary = "分页查询小区")
    @RequirePermission("property:community:list")
    @GetMapping("/list")
    public Result<PageResult<Community>> list(PageQuery pageQuery,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(communityService.pageList(pageQuery, keyword));
    }

    @Operation(summary = "查询所有小区")
    @GetMapping("/all")
    public Result<List<Community>> listAll() {
        return Result.success(communityService.listAll());
    }

    @Operation(summary = "获取小区详情")
    @RequirePermission("property:community:list")
    @GetMapping("/{id}")
    public Result<Community> getById(@PathVariable Long id) {
        return Result.success(communityService.getById(id));
    }

    @OperateLog(module = "小区管理", operation = "新增")
    @Operation(summary = "新增小区")
    @RequirePermission("property:community:list")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Community community) {
        communityService.add(community);
        return Result.success();
    }

    @OperateLog(module = "小区管理", operation = "修改")
    @Operation(summary = "修改小区")
    @RequirePermission("property:community:list")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Community community) {
        communityService.update(community);
        return Result.success();
    }

    @OperateLog(module = "小区管理", operation = "删除")
    @Operation(summary = "删除小区")
    @RequirePermission("property:community:list")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        communityService.delete(id);
        return Result.success();
    }
}

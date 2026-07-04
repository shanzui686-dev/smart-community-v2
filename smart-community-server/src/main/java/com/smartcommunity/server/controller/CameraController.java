package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Camera;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.CameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "摄像头管理", description = "摄像头设备增删改查")
@RestController
@RequestMapping("/api/property/camera")
public class CameraController {

    private final CameraService cameraService;

    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @Operation(summary = "分页查询摄像头")
    @RequirePermission("property:camera:list")
    @GetMapping("/list")
    public Result<PageResult<Camera>> list(PageQuery pageQuery,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long communityId) {
        return Result.success(cameraService.pageList(pageQuery, keyword, communityId));
    }

    @Operation(summary = "获取摄像头详情")
    @RequirePermission("property:camera:list")
    @GetMapping("/{id}")
    public Result<Camera> getById(@PathVariable Long id) {
        return Result.success(cameraService.getById(id));
    }

    @OperateLog(module = "摄像头管理", operation = "新增")
    @Operation(summary = "新增摄像头")
    @RequirePermission("property:camera:list")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Camera camera) {
        cameraService.add(camera);
        return Result.success();
    }

    @OperateLog(module = "摄像头管理", operation = "修改")
    @Operation(summary = "修改摄像头")
    @RequirePermission("property:camera:list")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Camera camera) {
        cameraService.update(camera);
        return Result.success();
    }

    @OperateLog(module = "摄像头管理", operation = "删除")
    @Operation(summary = "删除摄像头")
    @RequirePermission("property:camera:list")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cameraService.delete(id);
        return Result.success();
    }
}

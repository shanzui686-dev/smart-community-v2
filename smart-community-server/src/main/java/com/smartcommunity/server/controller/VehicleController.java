package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Vehicle;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "车辆管理", description = "车辆信息增删改查")
@RestController
@RequestMapping("/api/property/vehicle")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "分页查询车辆")
    @RequirePermission("property:vehicle:list")
    @GetMapping("/list")
    public Result<PageResult<Vehicle>> list(PageQuery pageQuery,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long communityId) {
        return Result.success(vehicleService.pageList(pageQuery, keyword, communityId));
    }

    @Operation(summary = "获取车辆详情")
    @RequirePermission("property:vehicle:list")
    @GetMapping("/{id}")
    public Result<Vehicle> getById(@PathVariable Long id) {
        return Result.success(vehicleService.getById(id));
    }

    @Operation(summary = "新增车辆")
    @RequirePermission("property:vehicle:list")
    @OperateLog(module = "车辆管理", operation = "新增")
    @PostMapping
    public Result<Void> add(@RequestBody Vehicle vehicle) {
        vehicleService.add(vehicle);
        return Result.success();
    }

    @Operation(summary = "修改车辆")
    @RequirePermission("property:vehicle:list")
    @OperateLog(module = "车辆管理", operation = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Vehicle vehicle) {
        vehicleService.update(vehicle);
        return Result.success();
    }

    @Operation(summary = "删除车辆")
    @RequirePermission("property:vehicle:list")
    @OperateLog(module = "车辆管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return Result.success();
    }
}
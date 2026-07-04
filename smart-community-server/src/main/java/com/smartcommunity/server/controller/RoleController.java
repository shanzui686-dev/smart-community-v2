package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Role;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "角色管理", description = "角色增删改查及权限分配")
@RestController
@RequestMapping("/api/system/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "查询所有角色")
    @RequirePermission("system:role:list")
    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "获取角色详情")
    @RequirePermission("system:role:list")
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @RequirePermission("system:role:list")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Role role) {
        roleService.add(role);
        return Result.success();
    }

    @Operation(summary = "修改角色")
    @RequirePermission("system:role:list")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Role role) {
        roleService.update(role);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @RequirePermission("system:role:list")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "分配角色菜单权限")
    @RequirePermission("system:role:list")
    @PutMapping("/{roleId}/menus")
    public Result<Void> assignMenus(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> params) {
        roleService.assignMenus(roleId, params.get("menuIds"));
        return Result.success();
    }

    @Operation(summary = "获取角色的菜单ID列表")
    @RequirePermission("system:role:list")
    @GetMapping("/{roleId}/menu-ids")
    public Result<List<Long>> getRoleMenuIds(@PathVariable Long roleId) {
        return Result.success(roleService.getRoleMenuIds(roleId));
    }
}

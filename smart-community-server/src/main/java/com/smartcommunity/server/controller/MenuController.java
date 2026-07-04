package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Menu;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理", description = "菜单及按钮权限管理")
@RestController
@RequestMapping("/api/system/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "查询所有菜单")
    @RequirePermission("system:menu:list")
    @GetMapping("/list")
    public Result<List<Menu>> list() {
        return Result.success(menuService.listAll());
    }

    @Operation(summary = "获取菜单详情")
    @RequirePermission("system:menu:list")
    @GetMapping("/{id}")
    public Result<Menu> getById(@PathVariable Long id) {
        return Result.success(menuService.getById(id));
    }

    @Operation(summary = "新增菜单")
    @RequirePermission("system:menu:list")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Menu menu) {
        menuService.add(menu);
        return Result.success();
    }

    @Operation(summary = "修改菜单")
    @RequirePermission("system:menu:list")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Menu menu) {
        menuService.update(menu);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @RequirePermission("system:menu:list")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}

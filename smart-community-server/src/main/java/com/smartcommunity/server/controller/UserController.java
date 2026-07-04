package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.User;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理", description = "系统用户增删改查")
@RestController
@RequestMapping("/api/system/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户")
    @RequirePermission("system:user:list")
    @GetMapping("/list")
    public Result<PageResult<User>> list(PageQuery pageQuery,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status) {
        return Result.success(userService.pageList(pageQuery, keyword, status));
    }

    @Operation(summary = "获取用户详情")
    @RequirePermission("system:user:list")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @RequirePermission("system:user:add")
    @OperateLog(module = "用户管理", operation = "新增")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody User user) {
        userService.add(user);
        return Result.success();
    }

    @Operation(summary = "修改用户")
    @RequirePermission("system:user:edit")
    @OperateLog(module = "用户管理", operation = "修改")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody User user) {
        userService.update(user);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @RequirePermission("system:user:delete")
    @OperateLog(module = "用户管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody Map<String, String> params) {
        Long id = Long.parseLong(params.get("userId"));
        userService.updatePassword(id, params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }

    @Operation(summary = "重置密码（管理员）")
    @RequirePermission("system:user:edit")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        userService.resetPassword(id, params.get("newPassword"));
        return Result.success();
    }
}

package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.dto.LoginDTO;
import com.smartcommunity.server.service.AuthService;
import com.smartcommunity.server.vo.LoginVO;
import com.smartcommunity.server.vo.MenuTreeVO;
import com.smartcommunity.server.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "认证管理", description = "登录、退出、用户信息")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    @Operation(summary = "用户退出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return authService.logout(userId);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public Result<UserInfoVO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return authService.getUserInfo(userId);
    }

    @Operation(summary = "获取当前用户菜单")
    @GetMapping("/user-menus")
    public Result<List<MenuTreeVO>> getUserMenus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return authService.getUserMenus(userId);
    }

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return authService.getCaptcha();
    }
}

package com.smartcommunity.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 权限拦截器 —— 基于 @RequirePermission 注解校验用户权限
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PermissionInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return true;
        }

        // 超级管理员(userId=1)跳过权限检查
        if (userId == 1L) {
            return true;
        }

        // 仅处理方法级别的请求
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 先查方法上的注解，再查类上的注解
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (annotation == null) {
            return true; // 无注解则放行
        }

        // 检查用户是否拥有所需权限
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);
        if (permissions != null && permissions.contains(annotation.value())) {
            return true;
        }

        // 无权限，返回 403
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(403, "没有操作权限")
        ));
        return false;
    }
}

package com.smartcommunity.server.security;

import cn.hutool.core.util.StrUtil;
import com.smartcommunity.server.utils.RedisUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Component
public class JwtFilter implements Filter {

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Value("${jwt.expire}")
    private Long expire;

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /** 无需认证的路径 */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/captcha",
            "/doc.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/favicon.ico",
            "/error"
    );

    public JwtFilter(JwtUtil jwtUtil, RedisUtil redisUtil) {
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 放行不需要认证的路径
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 获取Token
        String token = httpRequest.getHeader(header);
        if (StrUtil.isBlank(token) || !token.startsWith(tokenPrefix)) {
            sendError(httpResponse, 401, "未登录或Token已过期");
            return;
        }

        token = token.substring(tokenPrefix.length()).trim();

        // 验证Token
        try {
            if (jwtUtil.isExpired(token)) {
                sendError(httpResponse, 401, "Token已过期，请重新登录");
                return;
            }

            // 检查Redis中是否存在（支持主动失效，Redis不可用时跳过）
            Long userId = jwtUtil.getUserId(token);
            try {
                String cacheToken = String.valueOf(redisUtil.get("token:" + userId));
                if (StrUtil.isBlank(cacheToken) || "null".equals(cacheToken)) {
                    sendError(httpResponse, 401, "Token已失效，请重新登录");
                    return;
                }
            } catch (Exception e) {
                // Redis不可用时降级，仅依赖JWT签名验证
            }

            // 将用户信息设置到请求属性中
            httpRequest.setAttribute("userId", userId);
            httpRequest.setAttribute("username", jwtUtil.getUsername(token));

            // 续期Token（如果剩余时间小于一半）
            if (shouldRefresh(token)) {
                String newToken = refreshToken(token);
                httpResponse.setHeader(header, tokenPrefix + " " + newToken);
            }

        } catch (Exception e) {
            sendError(httpResponse, 401, "Token验证失败");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldRefresh(String token) {
        // 简化处理，不自动续期
        return false;
    }

    private String refreshToken(String token) {
        // 暂不实现自动续期
        return token;
    }

    private void sendError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\"}");
    }
}

package com.smartcommunity.server.security;

import com.smartcommunity.server.entity.OperationLog;
import com.smartcommunity.server.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class OperateLogAspect {

    private final OperationLogMapper operationLogMapper;

    public OperateLogAspect(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Around("@annotation(com.smartcommunity.server.security.OperateLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        int status = 1; // 成功

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = 0; // 失败
            throw e;
        } finally {
            try {
                long duration = System.currentTimeMillis() - start;
                saveLog(joinPoint, duration, status);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, long duration, int status) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperateLog annotation = method.getAnnotation(OperateLog.class);
        if (annotation == null) return;

        HttpServletRequest request = null;
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) request = attrs.getRequest();
        } catch (Exception ignored) {}

        // 从请求属性中获取当前用户（JwtFilter 设置）
        String username = null;
        Long userId = null;
        if (request != null) {
            Object uid = request.getAttribute("userId");
            Object uname = request.getAttribute("username");
            if (uid instanceof Long) userId = (Long) uid;
            if (uname != null) username = uname.toString();
        }

        String params = "";
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof HttpServletRequest || args[i] instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                if (sb.length() > 0) sb.append(", ");
                String s = args[i] != null ? args[i].toString() : "null";
                sb.append(s.length() > 200 ? s.substring(0, 200) + "..." : s);
            }
            params = sb.toString();
        }

        String ip = "";
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            ip = forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        }

        OperationLog logEntry = new OperationLog();
        logEntry.setUserId(userId);
        logEntry.setUsername(username);
        logEntry.setModule(annotation.module());
        logEntry.setOperation(annotation.operation());
        logEntry.setMethod(signature.getDeclaringTypeName() + "." + method.getName());
        logEntry.setParams(params);
        logEntry.setIp(ip);
        logEntry.setDuration(duration);
        logEntry.setStatus(status);
        operationLogMapper.insert(logEntry);
    }
}

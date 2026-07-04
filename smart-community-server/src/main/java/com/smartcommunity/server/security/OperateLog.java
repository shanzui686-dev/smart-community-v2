package com.smartcommunity.server.security;

import java.lang.annotation.*;

/**
 * 操作日志注解，标记需要记录操作日志的 Controller 方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {
    /** 操作模块 */
    String module();
    /** 操作类型 */
    String operation();
}

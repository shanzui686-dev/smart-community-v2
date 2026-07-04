package com.smartcommunity.server.common;

/**
 * 通用状态枚举
 */
public enum StatusEnum {
    DISABLE(0, "禁用"),
    ENABLE(1, "启用");

    private final Integer code;
    private final String desc;

    StatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

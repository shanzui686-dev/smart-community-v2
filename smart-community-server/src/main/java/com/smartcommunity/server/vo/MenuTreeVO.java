package com.smartcommunity.server.vo;

import lombok.Data;
import java.util.List;

@Data
public class MenuTreeVO {
    private Long menuId;
    private String menuName;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer type;
    private String permission;
    private List<MenuTreeVO> children;
}

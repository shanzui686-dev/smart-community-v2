package com.smartcommunity.server.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String mobile;
    private String email;
    private List<String> roles;
    private List<String> permissions;
}

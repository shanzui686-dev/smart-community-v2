package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long userId;
    private String username;
    private String password;
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    private String avatar;
    private String email;
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 用户角色ID列表（非数据库字段，用于接收前端传参） */
    @TableField(exist = false)
    private List<Long> roleIds;
}

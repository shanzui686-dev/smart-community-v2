package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("visitor")
public class Visitor implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long visitorId;
    private String name;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    private String idCard;
    private Long communityId;
    private String houseNo;
    private Long personId;
    private String faceUrl;
    private LocalDateTime visitTime;
    private LocalDateTime leaveTime;

    /** 签到时间（visitTime 在签到后被覆盖，此处用作显示别名） */
    @TableField(exist = false)
    private LocalDateTime checkInTime;
    /** 所属小区名称（非DB字段，查询时填充） */
    @TableField(exist = false)
    private String communityName;
    private String reason;
    private String plateNumber;
    private Integer visitorCount;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

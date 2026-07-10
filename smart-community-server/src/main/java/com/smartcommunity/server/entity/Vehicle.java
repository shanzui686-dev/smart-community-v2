package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("vehicle")
public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long vehicleId;
    private Long personId;
    private Long communityId;
    private String plateNumber;
    private Integer vehicleType;
    private Integer hasParkingSpace;
    private String remark;
    @TableField(exist = false)
    private String personName;
    @TableField(exist = false)
    private String communityName;
    @TableField(exist = false)
    private String houseNo;
    @TableField(exist = false)
    private String mobile;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
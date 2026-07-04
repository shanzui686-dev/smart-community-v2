package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("camera")
public class Camera implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long cameraId;
    private String name;
    private String deviceCode;
    private String ipAddress;
    private Long communityId;
    private String location;
    private String streamUrl;
    private Integer deviceType;
    private Integer onlineStatus;
    private Integer status;

    /** 小区名称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String communityName;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

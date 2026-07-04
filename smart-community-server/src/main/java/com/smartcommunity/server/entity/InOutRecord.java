package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("in_out_record")
public class InOutRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long personId;
    private String personName;
    private Long communityId;
    private Long cameraId;
    private Integer type;
    private LocalDateTime time;
    private String location;
    private String photoUrl;
    private Integer verifyType;
    private Integer verified;

    /** 外出时间（非数据库字段，合并记录时填充） */
    @TableField(exist = false)
    private LocalDateTime outTime;

    /** 所属小区名称（非DB字段，查询时填充） */
    @TableField(exist = false)
    private String communityName;
    /** 门牌号（非DB字段，查询时填充） */
    @TableField(exist = false)
    private String houseNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

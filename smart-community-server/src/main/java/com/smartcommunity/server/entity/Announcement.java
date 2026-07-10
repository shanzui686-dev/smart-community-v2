package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long announcementId;
    
    private Long communityId;
    
    private String title;
    
    private String content;
    
    private Integer status;
    
    private Integer expired;
    
    private Integer top;
    
    private Long createBy;
    
    private String createByName;
    
    @TableField(exist = false)
    private String communityName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}

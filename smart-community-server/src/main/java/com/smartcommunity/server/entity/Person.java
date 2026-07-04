package com.smartcommunity.server.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("person")
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    @ExcelProperty("ID")
    private Long personId;

    @ExcelIgnore
    private Long communityId;

    @ExcelProperty("姓名")
    private String userName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("性别")
    private Integer sex;

    @ExcelProperty("门牌号")
    private String houseNo;

    @ExcelProperty("人员类型")
    private Integer personType;

    @ExcelIgnore
    private String faceUrl;

    @ExcelIgnore
    private String faceId;

    @ExcelProperty("在住状态")
    private Integer state;

    @ExcelProperty("备注")
    private String remark;

    /** 小区名称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    @ExcelProperty("所属小区")
    private String communityName;

    @ExcelIgnore
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ExcelProperty("更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

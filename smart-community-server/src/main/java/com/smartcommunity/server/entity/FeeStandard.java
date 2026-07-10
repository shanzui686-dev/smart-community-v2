package com.smartcommunity.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fee_standard")
public class FeeStandard implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long feeStandardId;

    /** 费用名称：物业费、停车费、水电公摊等 */
    private String feeName;

    /** 单价（元） */
    private BigDecimal unitPrice;

    /** 计算方式：1-按户固定，2-按面积 */
    private Integer calculationMethod;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

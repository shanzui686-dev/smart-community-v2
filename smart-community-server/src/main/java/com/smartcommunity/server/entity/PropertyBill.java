package com.smartcommunity.server.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("property_bill")
public class PropertyBill implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ExcelProperty("账单编号")
    private Long billId;

    /** 关联住户ID */
    private Long personId;

    /** 关联小区ID */
    private Long communityId;

    /** 门牌号 */
    @ExcelProperty("房屋号")
    private String houseNo;

    /** 关联计费标准ID */
    private Long feeStandardId;

    /** 费用名称 */
    @ExcelProperty("费用类型")
    private String feeName;

    /** 应缴金额 */
    @ExcelProperty("应缴金额")
    private BigDecimal amount;

    /** 账单月份（格式：YYYY-MM） */
    @ExcelProperty("账单月份")
    private String billMonth;

    /** 到期时间 */
    @ExcelProperty("到期日期")
    private LocalDate dueDate;

    /** 状态：0-待缴，1-已缴，2-逾期 */
    @ExcelProperty("状态")
    private Integer status;

    /** 最后催缴时间 */
    private LocalDateTime lastDunningTime;

    @TableField(exist = false)
    @ExcelProperty("住户姓名")
    private String personName;

    @TableField(exist = false)
    @ExcelProperty("小区名称")
    private String communityName;

    @TableField(exist = false)
    @ExcelProperty("状态描述")
    private String statusText;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
package com.smartcommunity.server.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ExcelProperty("流水单号")
    private Long paymentId;

    /** 关联账单ID */
    @ExcelProperty("账单编号")
    private Long billId;

    /** 实缴金额 */
    @ExcelProperty("实缴金额")
    private BigDecimal paidAmount;

    /** 支付方式：支付宝/微信/现金/银行转账 */
    @ExcelProperty("支付方式")
    private String payMethod;

    /** 支付时间 */
    @ExcelProperty("支付时间")
    private LocalDateTime payTime;

    /** 操作人 */
    @ExcelProperty("操作人")
    private String operator;

    /** 备注 */
    @ExcelProperty("备注")
    private String remark;

    @TableField(exist = false)
    @ExcelProperty("费用类型")
    private String feeName;

    @TableField(exist = false)
    @ExcelProperty("账单月份")
    private String billMonth;

    @TableField(exist = false)
    @ExcelProperty("房屋号")
    private String houseNo;

    @TableField(exist = false)
    @ExcelProperty("住户姓名")
    private String personName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
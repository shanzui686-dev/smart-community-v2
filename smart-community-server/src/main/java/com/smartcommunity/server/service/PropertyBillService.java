package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.entity.PropertyBill;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;

public interface PropertyBillService {
    PageResult<PropertyBill> pageList(PageQuery pageQuery, Long communityId, String personName,
                                       String houseNo, Integer status, String billMonth);
    PropertyBill getById(Long id);
    void add(PropertyBill bill);
    void update(PropertyBill bill);
    void delete(Long id);

    /** 收银核销：缴费 */
    void payBill(Long billId, BigDecimal amount, String payMethod);

    /** 催缴通知 */
    void sendDunningNotice(Long billId);

    /** 查询某住户历史缴费明细 */
    List<PropertyBill> listHistoryByPersonId(Long personId);

    /** 定时任务：每月1号自动生成账单 */
    void generateMonthlyBills();

    /** 导出Excel */
    void exportExcel(HttpServletResponse response, Long communityId, String personName, String houseNo, Integer status, String billMonth);
}

package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.entity.FinancialReportDTO;
import com.smartcommunity.server.entity.PaymentRecord;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface PaymentRecordService {
    PageResult<PaymentRecord> pageList(PageQuery pageQuery, Long billId, String payMethod,
                                        String startTime, String endTime, String keyword);
    List<PaymentRecord> listByBillId(Long billId);
    void add(PaymentRecord record);
    FinancialReportDTO getReportData();
    void exportExcel(HttpServletResponse response, Long billId, String payMethod, String startTime, String endTime, String keyword);
}

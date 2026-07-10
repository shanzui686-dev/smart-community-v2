package com.smartcommunity.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.entity.PaymentRecord;
import com.smartcommunity.server.entity.PropertyBill;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.entity.FinancialReportDTO;
import com.smartcommunity.server.mapper.PaymentRecordMapper;
import com.smartcommunity.server.mapper.PropertyBillMapper;
import com.smartcommunity.server.mapper.PersonMapper;
import com.smartcommunity.server.service.PaymentRecordService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final PropertyBillMapper propertyBillMapper;
    private final PersonMapper personMapper;

    public PaymentRecordServiceImpl(PaymentRecordMapper paymentRecordMapper,
                                     PropertyBillMapper propertyBillMapper,
                                     PersonMapper personMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.propertyBillMapper = propertyBillMapper;
        this.personMapper = personMapper;
    }

    @Override
    public PageResult<PaymentRecord> pageList(PageQuery pageQuery, Long billId, String payMethod,
                                               String startTime, String endTime, String keyword) {
        Page<PaymentRecord> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (billId != null) {
            wrapper.eq(PaymentRecord::getBillId, billId);
        }
        if (payMethod != null && !payMethod.isEmpty()) {
            wrapper.eq(PaymentRecord::getPayMethod, payMethod);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(PaymentRecord::getPayTime, startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(PaymentRecord::getPayTime, endTime + " 23:59:59");
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime);
        Page<PaymentRecord> result = paymentRecordMapper.selectPage(page, wrapper);
        // 填充费用名称和账单月份
        for (PaymentRecord record : result.getRecords()) {
            fillExtraInfo(record);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public List<PaymentRecord> listByBillId(Long billId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getBillId, billId);
        wrapper.orderByDesc(PaymentRecord::getCreateTime);
        List<PaymentRecord> records = paymentRecordMapper.selectList(wrapper);
        for (PaymentRecord record : records) {
            fillExtraInfo(record);
        }
        return records;
    }

    @Override
    @Transactional
    public void add(PaymentRecord record) {
        paymentRecordMapper.insert(record);
    }

    private void fillExtraInfo(PaymentRecord record) {
        if (record.getBillId() != null) {
            PropertyBill bill = propertyBillMapper.selectById(record.getBillId());
            if (bill != null) {
                record.setFeeName(bill.getFeeName());
                record.setBillMonth(bill.getBillMonth());
                record.setHouseNo(bill.getHouseNo());
                if (bill.getPersonId() != null) {
                    Person person = personMapper.selectById(bill.getPersonId());
                    if (person != null) {
                        record.setPersonName(person.getUserName());
                    }
                }
            }
        }
    }

    @Override
    public FinancialReportDTO getReportData() {
        FinancialReportDTO report = new FinancialReportDTO();
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String currentYear = currentMonth.substring(0, 4);

        // 本月总应收：本月所有账单金额之和
        BigDecimal totalReceivable = propertyBillMapper.selectList(
                new LambdaQueryWrapper<PropertyBill>().eq(PropertyBill::getBillMonth, currentMonth))
                .stream().map(PropertyBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalReceivable(totalReceivable);

        int currentMonthLastDay = YearMonth.now().lengthOfMonth();
        // 本月实收：本月所有缴费记录金额之和
        BigDecimal actualReceived = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>().ge(PaymentRecord::getPayTime, currentMonth + "-01 00:00:00")
                        .le(PaymentRecord::getPayTime, currentMonth + "-" + currentMonthLastDay + " 23:59:59"))
                .stream().map(PaymentRecord::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setActualReceived(actualReceived);

        // 收缴率
        BigDecimal collectionRate = totalReceivable.compareTo(BigDecimal.ZERO) > 0
                ? actualReceived.multiply(BigDecimal.valueOf(100)).divide(totalReceivable, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        report.setCollectionRate(collectionRate);

        // 历史欠费总额：非本月的未缴账单金额之和
        BigDecimal historyArrears = propertyBillMapper.selectList(
                new LambdaQueryWrapper<PropertyBill>().ne(PropertyBill::getBillMonth, currentMonth)
                        .lt(PropertyBill::getStatus, 1))
                .stream().map(PropertyBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setHistoryArrears(historyArrears);

        // 待缴账单数
        Integer billCount = propertyBillMapper.selectCount(
                new LambdaQueryWrapper<PropertyBill>().eq(PropertyBill::getBillMonth, currentMonth)).intValue();
        report.setBillCount(billCount);

        // 已缴账单数
        Integer paidCount = propertyBillMapper.selectCount(
                new LambdaQueryWrapper<PropertyBill>().eq(PropertyBill::getBillMonth, currentMonth)
                        .eq(PropertyBill::getStatus, 1)).intValue();
        report.setPaidCount(paidCount);

        // 逾期账单数
        Integer overdueCount = propertyBillMapper.selectCount(
                new LambdaQueryWrapper<PropertyBill>().eq(PropertyBill::getStatus, 2)).intValue();
        report.setOverdueCount(overdueCount);

        // 近6个月收入趋势
        report.setMonthlyIncomeList(getMonthlyIncomeTrend());

        // 本年度各费用类型收入占比
        report.setFeeTypeIncomeList(getFeeTypeIncomeBreakdown(currentYear));

        return report;
    }

    private List<FinancialReportDTO.MonthlyIncomeDTO> getMonthlyIncomeTrend() {
        List<FinancialReportDTO.MonthlyIncomeDTO> list = new ArrayList<>();
        YearMonth current = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            String monthStr = month.format(formatter);

            FinancialReportDTO.MonthlyIncomeDTO dto = new FinancialReportDTO.MonthlyIncomeDTO();
            dto.setMonth(monthStr);
            dto.setPropertyFee(BigDecimal.ZERO);
            dto.setParkingFee(BigDecimal.ZERO);
            dto.setUtilityFee(BigDecimal.ZERO);
            dto.setOtherFee(BigDecimal.ZERO);

            int lastDay = month.lengthOfMonth();
            List<PaymentRecord> records = paymentRecordMapper.selectList(
                    new LambdaQueryWrapper<PaymentRecord>().ge(PaymentRecord::getPayTime, monthStr + "-01 00:00:00")
                            .le(PaymentRecord::getPayTime, monthStr + "-" + lastDay + " 23:59:59"));

            for (PaymentRecord record : records) {
                fillExtraInfo(record);
                String feeName = record.getFeeName();
                BigDecimal amount = record.getPaidAmount();
                if (feeName != null) {
                    if (feeName.contains("物业")) {
                        dto.setPropertyFee(dto.getPropertyFee().add(amount));
                    } else if (feeName.contains("停车")) {
                        dto.setParkingFee(dto.getParkingFee().add(amount));
                    } else if (feeName.contains("水电") || feeName.contains("公摊") || feeName.contains("垃圾")) {
                        dto.setUtilityFee(dto.getUtilityFee().add(amount));
                    } else {
                        dto.setOtherFee(dto.getOtherFee().add(amount));
                    }
                } else {
                    dto.setOtherFee(dto.getOtherFee().add(amount));
                }
            }

            dto.setTotal(dto.getPropertyFee().add(dto.getParkingFee()).add(dto.getUtilityFee()).add(dto.getOtherFee()));
            list.add(dto);
        }

        return list;
    }

    private List<FinancialReportDTO.FeeTypeIncomeDTO> getFeeTypeIncomeBreakdown(String year) {
        List<FinancialReportDTO.FeeTypeIncomeDTO> list = new ArrayList<>();

        List<PaymentRecord> records = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>().ge(PaymentRecord::getPayTime, year + "-01-01 00:00:00")
                        .le(PaymentRecord::getPayTime, year + "-12-31 23:59:59"));

        BigDecimal total = records.stream().map(PaymentRecord::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal propertyFee = BigDecimal.ZERO;
        BigDecimal parkingFee = BigDecimal.ZERO;
        BigDecimal utilityFee = BigDecimal.ZERO;
        BigDecimal garbageFee = BigDecimal.ZERO;
        BigDecimal otherFee = BigDecimal.ZERO;

        for (PaymentRecord record : records) {
            fillExtraInfo(record);
            String feeName = record.getFeeName();
            BigDecimal amount = record.getPaidAmount();
            if (feeName != null) {
                if (feeName.contains("物业")) {
                    propertyFee = propertyFee.add(amount);
                } else if (feeName.contains("停车")) {
                    parkingFee = parkingFee.add(amount);
                } else if (feeName.contains("水电") || feeName.contains("公摊")) {
                    utilityFee = utilityFee.add(amount);
                } else if (feeName.contains("垃圾")) {
                    garbageFee = garbageFee.add(amount);
                } else {
                    otherFee = otherFee.add(amount);
                }
            } else {
                otherFee = otherFee.add(amount);
            }
        }

        if (propertyFee.compareTo(BigDecimal.ZERO) > 0) {
            FinancialReportDTO.FeeTypeIncomeDTO dto = new FinancialReportDTO.FeeTypeIncomeDTO();
            dto.setFeeName("物业费");
            dto.setAmount(propertyFee);
            dto.setPercentage(total.compareTo(BigDecimal.ZERO) > 0
                    ? propertyFee.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            list.add(dto);
        }

        if (parkingFee.compareTo(BigDecimal.ZERO) > 0) {
            FinancialReportDTO.FeeTypeIncomeDTO dto = new FinancialReportDTO.FeeTypeIncomeDTO();
            dto.setFeeName("停车费");
            dto.setAmount(parkingFee);
            dto.setPercentage(total.compareTo(BigDecimal.ZERO) > 0
                    ? parkingFee.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            list.add(dto);
        }

        if (utilityFee.compareTo(BigDecimal.ZERO) > 0) {
            FinancialReportDTO.FeeTypeIncomeDTO dto = new FinancialReportDTO.FeeTypeIncomeDTO();
            dto.setFeeName("水电公摊");
            dto.setAmount(utilityFee);
            dto.setPercentage(total.compareTo(BigDecimal.ZERO) > 0
                    ? utilityFee.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            list.add(dto);
        }

        if (garbageFee.compareTo(BigDecimal.ZERO) > 0) {
            FinancialReportDTO.FeeTypeIncomeDTO dto = new FinancialReportDTO.FeeTypeIncomeDTO();
            dto.setFeeName("垃圾清运费");
            dto.setAmount(garbageFee);
            dto.setPercentage(total.compareTo(BigDecimal.ZERO) > 0
                    ? garbageFee.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            list.add(dto);
        }

        if (otherFee.compareTo(BigDecimal.ZERO) > 0) {
            FinancialReportDTO.FeeTypeIncomeDTO dto = new FinancialReportDTO.FeeTypeIncomeDTO();
            dto.setFeeName("其他");
            dto.setAmount(otherFee);
            dto.setPercentage(total.compareTo(BigDecimal.ZERO) > 0
                    ? otherFee.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            list.add(dto);
        }

        return list;
    }

    @Override
    public void exportExcel(HttpServletResponse response, Long billId, String payMethod, String startTime, String endTime, String keyword) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("收支流水明细", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
            if (billId != null) {
                wrapper.eq(PaymentRecord::getBillId, billId);
            }
            if (payMethod != null && !payMethod.isEmpty()) {
                wrapper.eq(PaymentRecord::getPayMethod, payMethod);
            }
            if (startTime != null && !startTime.isEmpty()) {
                wrapper.ge(PaymentRecord::getPayTime, startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                wrapper.le(PaymentRecord::getPayTime, endTime);
            }
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.like(PaymentRecord::getBillId, keyword)
                        .or().like(PaymentRecord::getPayMethod, keyword);
            }
            wrapper.orderByDesc(PaymentRecord::getPayTime);

            List<PaymentRecord> list = paymentRecordMapper.selectList(wrapper);
            for (PaymentRecord record : list) {
                fillExtraInfo(record);
            }

            EasyExcel.write(response.getOutputStream(), PaymentRecord.class)
                    .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                    .sheet("收支流水明细")
                    .doWrite(list);
        } catch (IOException e) {
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }
}

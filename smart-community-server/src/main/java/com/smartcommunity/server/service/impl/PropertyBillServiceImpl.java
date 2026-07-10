package com.smartcommunity.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.entity.FeeStandard;
import com.smartcommunity.server.entity.PaymentRecord;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.entity.PropertyBill;
import com.smartcommunity.server.mapper.PersonMapper;
import com.smartcommunity.server.mapper.PropertyBillMapper;
import com.smartcommunity.server.mapper.FeeStandardMapper;
import com.smartcommunity.server.mapper.PaymentRecordMapper;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.service.PropertyBillService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PropertyBillServiceImpl implements PropertyBillService {

    private static final Logger log = LoggerFactory.getLogger(PropertyBillServiceImpl.class);

    private final PropertyBillMapper propertyBillMapper;
    private final FeeStandardMapper feeStandardMapper;
    private final PersonMapper personMapper;
    private final CommunityMapper communityMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public PropertyBillServiceImpl(PropertyBillMapper propertyBillMapper,
                                    FeeStandardMapper feeStandardMapper,
                                    PersonMapper personMapper,
                                    CommunityMapper communityMapper,
                                    PaymentRecordMapper paymentRecordMapper) {
        this.propertyBillMapper = propertyBillMapper;
        this.feeStandardMapper = feeStandardMapper;
        this.personMapper = personMapper;
        this.communityMapper = communityMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    @Override
    public PageResult<PropertyBill> pageList(PageQuery pageQuery, Long communityId, String personName,
                                              String houseNo, Integer status, String billMonth) {
        Page<PropertyBill> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<PropertyBill> wrapper = new LambdaQueryWrapper<>();
        if (communityId != null) {
            wrapper.eq(PropertyBill::getCommunityId, communityId);
        }
        if (personName != null && !personName.isEmpty()) {
            wrapper.like(PropertyBill::getPersonName, personName);
        }
        if (houseNo != null && !houseNo.isEmpty()) {
            wrapper.like(PropertyBill::getHouseNo, houseNo);
        }
        if (status != null) {
            wrapper.eq(PropertyBill::getStatus, status);
        }
        if (billMonth != null && !billMonth.isEmpty()) {
            wrapper.eq(PropertyBill::getBillMonth, billMonth);
        }
        wrapper.orderByDesc(PropertyBill::getCreateTime);
        Page<PropertyBill> result = propertyBillMapper.selectPage(page, wrapper);
        // 填充住户姓名、小区名称等额外信息
        for (PropertyBill bill : result.getRecords()) {
            fillExtraInfo(bill);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PropertyBill getById(Long id) {
        PropertyBill bill = propertyBillMapper.selectById(id);
        if (bill != null) {
            fillExtraInfo(bill);
        }
        return bill;
    }

    @Override
    @Transactional
    public void add(PropertyBill bill) {
        propertyBillMapper.insert(bill);
    }

    @Override
    @Transactional
    public void update(PropertyBill bill) {
        propertyBillMapper.updateById(bill);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        propertyBillMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void payBill(Long billId, BigDecimal amount, String payMethod) {
        // 1. 查询账单
        PropertyBill bill = propertyBillMapper.selectById(billId);
        if (bill == null) {
            throw new RuntimeException("账单不存在");
        }
        if (bill.getStatus() == 1) {
            throw new RuntimeException("该账单已缴费，请勿重复操作");
        }

        // 2. 更新账单状态为已缴（1）
        bill.setStatus(1);
        propertyBillMapper.updateById(bill);

        // 3. 写入缴费流水
        PaymentRecord record = new PaymentRecord();
        record.setBillId(billId);
        record.setPaidAmount(amount);
        record.setPayMethod(payMethod);
        record.setPayTime(LocalDateTime.now());
        record.setOperator("system");
        paymentRecordMapper.insert(record);

        log.info("缴费成功：billId={}, amount={}, payMethod={}", billId, amount, payMethod);
    }

    @Override
    @Transactional
    public void sendDunningNotice(Long billId) {
        PropertyBill bill = propertyBillMapper.selectById(billId);
        if (bill == null) {
            throw new RuntimeException("账单不存在");
        }
        if (bill.getStatus() == 1) {
            throw new RuntimeException("该账单已缴费，无需催缴");
        }

        // 模拟发送催缴消息（打印日志）
        log.info("===== 催缴通知 =====");
        log.info("发送对象：住户ID={}", bill.getPersonId());
        log.info("门牌号：{}", bill.getHouseNo());
        log.info("费用类型：{}", bill.getFeeName());
        log.info("应缴金额：{}元", bill.getAmount());
        log.info("账单月份：{}", bill.getBillMonth());
        log.info("到期时间：{}", bill.getDueDate());
        log.info("====================");

        // 更新最后一次催缴时间
        bill.setLastDunningTime(LocalDateTime.now());
        propertyBillMapper.updateById(bill);
    }

    @Override
    public List<PropertyBill> listHistoryByPersonId(Long personId) {
        LambdaQueryWrapper<PropertyBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyBill::getPersonId, personId);
        wrapper.orderByDesc(PropertyBill::getCreateTime);
        List<PropertyBill> bills = propertyBillMapper.selectList(wrapper);
        for (PropertyBill bill : bills) {
            fillExtraInfo(bill);
        }
        return bills;
    }

    /**
     * 每月1号凌晨2点自动生成当月账单
     * 遍历所有启用的计费标准，为所有在住居民生成待缴账单
     */
    @Override
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void generateMonthlyBills() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("===== 开始生成{}月份账单 =====", currentMonth);

        // 1. 获取所有启用的计费标准
        List<FeeStandard> feeStandards = feeStandardMapper.selectList(
                new LambdaQueryWrapper<FeeStandard>().eq(FeeStandard::getStatus, 1));
        if (feeStandards.isEmpty()) {
            log.warn("没有启用的计费标准，跳过账单生成");
            return;
        }

        // 2. 获取所有在住居民
        List<Person> persons = personMapper.selectList(
                new LambdaQueryWrapper<Person>().eq(Person::getState, 1));
        if (persons.isEmpty()) {
            log.warn("没有在住居民，跳过账单生成");
            return;
        }

        int totalGenerated = 0;
        int totalSkipped = 0;

        for (Person person : persons) {
            for (FeeStandard standard : feeStandards) {
                // 防止重复生成：检查该住户该月该费用类型是否已有账单
                LambdaQueryWrapper<PropertyBill> existsWrapper = new LambdaQueryWrapper<>();
                existsWrapper.eq(PropertyBill::getPersonId, person.getPersonId())
                        .eq(PropertyBill::getFeeStandardId, standard.getFeeStandardId())
                        .eq(PropertyBill::getBillMonth, currentMonth);
                Long count = propertyBillMapper.selectCount(existsWrapper);
                if (count > 0) {
                    totalSkipped++;
                    continue;
                }

                // 创建账单
                PropertyBill bill = new PropertyBill();
                bill.setPersonId(person.getPersonId());
                bill.setCommunityId(person.getCommunityId());
                bill.setHouseNo(person.getHouseNo());
                bill.setFeeStandardId(standard.getFeeStandardId());
                bill.setFeeName(standard.getFeeName());
                bill.setAmount(standard.getUnitPrice());
                bill.setBillMonth(currentMonth);
                // 到期时间为当月最后一天
                LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
                bill.setDueDate(firstDay.plusMonths(1).minusDays(1));
                bill.setStatus(0); // 待缴
                propertyBillMapper.insert(bill);
                totalGenerated++;
            }
        }

        log.info("===== {}月份账单生成完成：生成{}条，跳过{}条（已存在）=====",
                currentMonth, totalGenerated, totalSkipped);
    }

    /**
     * 每天早上8点检查并更新逾期状态（超过到期时间未缴的账单标记为逾期）
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void updateOverdueStatus() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PropertyBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyBill::getStatus, 0)  // 待缴
                .lt(PropertyBill::getDueDate, today);  // 已过到期日
        List<PropertyBill> overdueBills = propertyBillMapper.selectList(wrapper);
        for (PropertyBill bill : overdueBills) {
            bill.setStatus(2); // 逾期
            propertyBillMapper.updateById(bill);
        }
        if (!overdueBills.isEmpty()) {
            log.info("已更新{}条逾期账单", overdueBills.size());
        }
    }

    private void fillExtraInfo(PropertyBill bill) {
        // 填充住户姓名
        if (bill.getPersonId() != null) {
            Person person = personMapper.selectById(bill.getPersonId());
            if (person != null) {
                bill.setPersonName(person.getUserName());
                if (bill.getCommunityName() == null) {
                    Community community = communityMapper.selectById(person.getCommunityId());
                    if (community != null) {
                        bill.setCommunityName(community.getName());
                    }
                }
            }
        }
        // 填充小区名称（如果已有communityId但上面没填上）
        if (bill.getCommunityName() == null && bill.getCommunityId() != null) {
            Community community = communityMapper.selectById(bill.getCommunityId());
            if (community != null) {
                bill.setCommunityName(community.getName());
            }
        }
        // 填充状态描述
        if (bill.getStatus() != null) {
            if (bill.getStatus() == 0) {
                bill.setStatusText("待缴");
            } else if (bill.getStatus() == 1) {
                bill.setStatusText("已缴清");
            } else if (bill.getStatus() == 2) {
                bill.setStatusText("逾期");
            }
        }
    }

    @Override
    public void exportExcel(HttpServletResponse response, Long communityId, String personName, String houseNo, Integer status, String billMonth) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("财务账单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            LambdaQueryWrapper<PropertyBill> wrapper = new LambdaQueryWrapper<>();
            if (communityId != null) {
                wrapper.eq(PropertyBill::getCommunityId, communityId);
            }
            if (personName != null && !personName.isEmpty()) {
                wrapper.like(PropertyBill::getPersonName, personName);
            }
            if (houseNo != null && !houseNo.isEmpty()) {
                wrapper.like(PropertyBill::getHouseNo, houseNo);
            }
            if (status != null) {
                wrapper.eq(PropertyBill::getStatus, status);
            }
            if (billMonth != null && !billMonth.isEmpty()) {
                wrapper.eq(PropertyBill::getBillMonth, billMonth);
            }
            wrapper.orderByDesc(PropertyBill::getCreateTime);

            List<PropertyBill> list = propertyBillMapper.selectList(wrapper);
            for (PropertyBill bill : list) {
                fillExtraInfo(bill);
            }

            EasyExcel.write(response.getOutputStream(), PropertyBill.class)
                    .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                    .sheet("财务账单")
                    .doWrite(list);
        } catch (IOException e) {
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }
}

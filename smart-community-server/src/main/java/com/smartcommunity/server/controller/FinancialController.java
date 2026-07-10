package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.FeeStandard;
import com.smartcommunity.server.entity.FinancialReportDTO;
import com.smartcommunity.server.entity.PaymentRecord;
import com.smartcommunity.server.entity.PropertyBill;
import com.smartcommunity.server.service.FeeStandardService;
import com.smartcommunity.server.service.PaymentRecordService;
import com.smartcommunity.server.service.PropertyBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "财务与缴费管理", description = "计费标准、账单、缴费流水管理")
@RestController
@RequestMapping("/api/financial")
public class FinancialController {

    private final FeeStandardService feeStandardService;
    private final PropertyBillService propertyBillService;
    private final PaymentRecordService paymentRecordService;

    public FinancialController(FeeStandardService feeStandardService,
                                PropertyBillService propertyBillService,
                                PaymentRecordService paymentRecordService) {
        this.feeStandardService = feeStandardService;
        this.propertyBillService = propertyBillService;
        this.paymentRecordService = paymentRecordService;
    }

    // ==================== 计费标准管理 ====================

    @Operation(summary = "分页查询计费标准")
    @GetMapping("/fee-standard/list")
    public Result<PageResult<FeeStandard>> feeStandardList(PageQuery pageQuery,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer status) {
        return Result.success(feeStandardService.pageList(pageQuery, keyword, status));
    }

    @Operation(summary = "查询启用的计费标准列表")
    @GetMapping("/fee-standard/enabled")
    public Result<List<FeeStandard>> feeStandardEnabled() {
        return Result.success(feeStandardService.listEnabled());
    }

    @Operation(summary = "获取计费标准详情")
    @GetMapping("/fee-standard/{id}")
    public Result<FeeStandard> feeStandardGetById(@PathVariable Long id) {
        return Result.success(feeStandardService.getById(id));
    }

    @Operation(summary = "新增计费标准")
    @PostMapping("/fee-standard")
    public Result<Void> feeStandardAdd(@RequestBody FeeStandard feeStandard) {
        feeStandardService.add(feeStandard);
        return Result.success();
    }

    @Operation(summary = "修改计费标准")
    @PutMapping("/fee-standard")
    public Result<Void> feeStandardUpdate(@RequestBody FeeStandard feeStandard) {
        feeStandardService.update(feeStandard);
        return Result.success();
    }

    @Operation(summary = "删除计费标准")
    @DeleteMapping("/fee-standard/{id}")
    public Result<Void> feeStandardDelete(@PathVariable Long id) {
        feeStandardService.delete(id);
        return Result.success();
    }

    // ==================== 账单管理 ====================

    @Operation(summary = "分页查询账单列表", description = "支持按小区、住户姓名、门牌号、状态、账单月份过滤")
    @GetMapping("/bill/list")
    public Result<PageResult<PropertyBill>> billList(PageQuery pageQuery,
                                                      @RequestParam(required = false) Long communityId,
                                                      @RequestParam(required = false) String personName,
                                                      @RequestParam(required = false) String houseNo,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String billMonth) {
        return Result.success(propertyBillService.pageList(pageQuery, communityId, personName, houseNo, status, billMonth));
    }

    @Operation(summary = "获取账单详情")
    @GetMapping("/bill/{id}")
    public Result<PropertyBill> billGetById(@PathVariable Long id) {
        return Result.success(propertyBillService.getById(id));
    }

    @Operation(summary = "新增账单（手动生成）")
    @PostMapping("/bill")
    public Result<Void> billAdd(@RequestBody PropertyBill bill) {
        propertyBillService.add(bill);
        return Result.success();
    }

    @Operation(summary = "修改账单")
    @PutMapping("/bill")
    public Result<Void> billUpdate(@RequestBody PropertyBill bill) {
        propertyBillService.update(bill);
        return Result.success();
    }

    @Operation(summary = "删除账单")
    @DeleteMapping("/bill/{id}")
    public Result<Void> billDelete(@PathVariable Long id) {
        propertyBillService.delete(id);
        return Result.success();
    }

    @Operation(summary = "查询某住户历史缴费明细")
    @GetMapping("/bill/history/{personId}")
    public Result<List<PropertyBill>> billHistory(@PathVariable Long personId) {
        return Result.success(propertyBillService.listHistoryByPersonId(personId));
    }

    @Operation(summary = "缴费核销")
    @PostMapping("/bill/{billId}/pay")
    public Result<Void> billPay(@PathVariable Long billId,
                                @RequestParam BigDecimal amount,
                                @RequestParam String payMethod) {
        propertyBillService.payBill(billId, amount, payMethod);
        return Result.success();
    }

    @Operation(summary = "发送催缴通知")
    @PostMapping("/bill/{billId}/dunning")
    public Result<Void> billDunning(@PathVariable Long billId) {
        propertyBillService.sendDunningNotice(billId);
        return Result.success();
    }

    @Operation(summary = "手动触发当月账单生成（仅测试用）")
    @PostMapping("/bill/generate")
    public Result<Void> billGenerate() {
        propertyBillService.generateMonthlyBills();
        return Result.success();
    }

    @Operation(summary = "导出账单Excel")
    @GetMapping("/bill/export")
    public void billExport(HttpServletResponse response,
                           @RequestParam(required = false) Long communityId,
                           @RequestParam(required = false) String personName,
                           @RequestParam(required = false) String houseNo,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) String billMonth) {
        propertyBillService.exportExcel(response, communityId, personName, houseNo, status, billMonth);
    }

    // ==================== 缴费流水管理 ====================

    @Operation(summary = "分页查询缴费流水")
    @GetMapping("/payment-record/list")
    public Result<PageResult<PaymentRecord>> paymentRecordList(PageQuery pageQuery,
                                                                @RequestParam(required = false) Long billId,
                                                                @RequestParam(required = false) String payMethod,
                                                                @RequestParam(required = false) String startTime,
                                                                @RequestParam(required = false) String endTime,
                                                                @RequestParam(required = false) String keyword) {
        return Result.success(paymentRecordService.pageList(pageQuery, billId, payMethod, startTime, endTime, keyword));
    }

    @Operation(summary = "查询某账单的缴费流水")
    @GetMapping("/payment-record/by-bill/{billId}")
    public Result<List<PaymentRecord>> paymentRecordByBill(@PathVariable Long billId) {
        return Result.success(paymentRecordService.listByBillId(billId));
    }

    @Operation(summary = "导出缴费流水Excel")
    @GetMapping("/payment-record/export")
    public void paymentRecordExport(HttpServletResponse response,
                                    @RequestParam(required = false) Long billId,
                                    @RequestParam(required = false) String payMethod,
                                    @RequestParam(required = false) String startTime,
                                    @RequestParam(required = false) String endTime,
                                    @RequestParam(required = false) String keyword) {
        paymentRecordService.exportExcel(response, billId, payMethod, startTime, endTime, keyword);
    }

    // ==================== 财务统计报表 ====================

    @Operation(summary = "获取财务统计报表数据")
    @GetMapping("/report")
    public Result<FinancialReportDTO> getReportData() {
        return Result.success(paymentRecordService.getReportData());
    }
}

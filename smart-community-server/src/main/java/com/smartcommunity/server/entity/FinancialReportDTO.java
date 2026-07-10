package com.smartcommunity.server.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FinancialReportDTO {

    private BigDecimal totalReceivable;

    private BigDecimal actualReceived;

    private BigDecimal collectionRate;

    private BigDecimal historyArrears;

    private Integer billCount;

    private Integer paidCount;

    private Integer overdueCount;

    private List<MonthlyIncomeDTO> monthlyIncomeList;

    private List<FeeTypeIncomeDTO> feeTypeIncomeList;

    @Data
    public static class MonthlyIncomeDTO {
        private String month;
        private BigDecimal propertyFee;
        private BigDecimal parkingFee;
        private BigDecimal utilityFee;
        private BigDecimal otherFee;
        private BigDecimal total;
    }

    @Data
    public static class FeeTypeIncomeDTO {
        private String feeName;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
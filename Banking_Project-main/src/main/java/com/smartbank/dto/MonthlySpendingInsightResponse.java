package com.smartbank.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlySpendingInsightResponse {

    private String month;
    private BigDecimal monthlySpend;
    private BigDecimal monthlyCredits;
    private BigDecimal previousMonthSpend;
    private Long transactionCount;
    private String analysis;
}

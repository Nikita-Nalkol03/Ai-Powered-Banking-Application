package com.smartbank.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardResponse {

    private long registeredCustomers;
    private long activeCustomers;
    private long blockedCustomers;
    private long totalAccounts;
    private long activeAccounts;
    private long blockedAccounts;
    private long closedAccounts;
    private long savingsAccounts;
    private long currentAccounts;
    private long successfulTransactionCount;
    private BigDecimal totalCreditVolume;
    private BigDecimal totalDebitVolume;
    private BigDecimal totalTransferVolume;
    private AdminActivitySummary dailyActivity;
    private AdminActivitySummary monthlyActivity;
    private List<AdminRecentTransactionResponse> recentTransactions;
}

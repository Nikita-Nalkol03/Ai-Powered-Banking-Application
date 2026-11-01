package com.smartbank.service;

import com.smartbank.dto.MonthlySpendingInsightResponse;

public interface SpendingInsightService {

    MonthlySpendingInsightResponse analyzeCurrentMonth(String accountNumber);
}

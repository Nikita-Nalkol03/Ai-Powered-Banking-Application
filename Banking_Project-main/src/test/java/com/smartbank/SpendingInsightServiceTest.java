package com.smartbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.smartbank.dto.TransactionSummaryResponse;
import com.smartbank.service.AiAssistantService;
import com.smartbank.service.TransactionHistoryService;
import com.smartbank.service.impl.SpendingInsightServiceImpl;

class SpendingInsightServiceTest {

    @Test
    void analyzesMonthlyAggregatesWithoutSendingAccountNumber() {
        TransactionHistoryService transactionHistory = mock(TransactionHistoryService.class);
        AiAssistantService aiAssistant = mock(AiAssistantService.class);
        when(transactionHistory.getAccountTransactionSummary(
                anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(
                        new TransactionSummaryResponse(
                                7L,
                                new BigDecimal("1250.00"),
                                new BigDecimal("3000.00"),
                                new BigDecimal("1750.00")),
                        new TransactionSummaryResponse(
                                5L,
                                new BigDecimal("900.00"),
                                new BigDecimal("2500.00"),
                                new BigDecimal("1600.00")));
        when(aiAssistant.answer(anyString())).thenReturn("Your monthly totals are ready.");

        SpendingInsightServiceImpl service = new SpendingInsightServiceImpl(
                transactionHistory, aiAssistant);
        var result = service.analyzeCurrentMonth("PRIVATE-ACCOUNT-123");

        assertEquals(new BigDecimal("1250.00"), result.getMonthlySpend());
        assertEquals(new BigDecimal("3000.00"), result.getMonthlyCredits());
        assertEquals(new BigDecimal("900.00"), result.getPreviousMonthSpend());
        assertEquals("Your monthly totals are ready.", result.getAnalysis());

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiAssistant).answer(prompt.capture());
        assertFalse(prompt.getValue().contains("PRIVATE-ACCOUNT-123"));
        assertFalse(prompt.getValue().contains("transactionReference"));
    }
}

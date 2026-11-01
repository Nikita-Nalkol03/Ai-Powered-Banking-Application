package com.smartbank.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.smartbank.dto.MonthlySpendingInsightResponse;
import com.smartbank.dto.TransactionSummaryResponse;
import com.smartbank.service.AiAssistantService;
import com.smartbank.service.SpendingInsightService;
import com.smartbank.service.TransactionHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpendingInsightServiceImpl implements SpendingInsightService {

    private final TransactionHistoryService transactionHistoryService;
    private final AiAssistantService aiAssistantService;

    @Override
    public MonthlySpendingInsightResponse analyzeCurrentMonth(String accountNumber) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        TransactionSummaryResponse currentSummary =
                transactionHistoryService.getAccountTransactionSummary(
                        accountNumber,
                        currentMonth.atDay(1),
                        today);
        TransactionSummaryResponse previousSummary =
                transactionHistoryService.getAccountTransactionSummary(
                        accountNumber,
                        previousMonth.atDay(1),
                        previousMonth.atEndOfMonth());

        String currentLabel = formatMonth(currentMonth);
        String previousLabel = formatMonth(previousMonth);
        String analysis = aiAssistantService.answer(buildPrompt(
                currentLabel, previousLabel, currentSummary, previousSummary));

        return new MonthlySpendingInsightResponse(
                currentLabel,
                currentSummary.getTotalDebit(),
                currentSummary.getTotalCredit(),
                previousSummary.getTotalDebit(),
                currentSummary.getTotalTransactions(),
                analysis);
    }

    private String buildPrompt(
            String currentMonth,
            String previousMonth,
            TransactionSummaryResponse current,
            TransactionSummaryResponse previous) {
        return """
                Analyze this customer's monthly banking totals in Indian rupees.
                Give a short, supportive explanation of spending and compare it
                with the previous month where meaningful. Mention one practical,
                general budgeting idea. Do not claim to know spending categories,
                merchants, or reasons because transaction details are not included.
                Do not give investment, tax, legal, or personalized financial advice.

                Current month: %s
                Current month debit total (spending): INR %s
                Current month credit total: INR %s
                Current month transaction count: %d
                Previous month: %s
                Previous month debit total (spending): INR %s
                """.formatted(
                currentMonth,
                current.getTotalDebit().toPlainString(),
                current.getTotalCredit().toPlainString(),
                current.getTotalTransactions(),
                previousMonth,
                previous.getTotalDebit().toPlainString());
    }

    private String formatMonth(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + month.getYear();
    }
}

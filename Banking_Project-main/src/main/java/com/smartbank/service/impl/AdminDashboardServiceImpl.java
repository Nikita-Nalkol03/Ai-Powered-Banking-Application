package com.smartbank.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartbank.dto.AdminActivitySummary;
import com.smartbank.dto.AdminDashboardResponse;
import com.smartbank.dto.AdminRecentTransactionResponse;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.BankTransaction;
import com.smartbank.entity.Role;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import com.smartbank.entity.UserStatus;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.AdminActivityProjection;
import com.smartbank.repository.BankTransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        AdminActivityProjection allTime = activity(
                LocalDate.of(1970, 1, 1), LocalDate.now().plusDays(1));
        LocalDate today = LocalDate.now();
        AdminActivityProjection daily = activity(today, today.plusDays(1));
        LocalDate monthStart = today.withDayOfMonth(1);
        AdminActivityProjection monthly = activity(monthStart, today.plusDays(1));

        List<AdminRecentTransactionResponse> recentTransactions =
                transactionRepository.findAll(PageRequest.of(
                                0,
                                8,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(this::toRecentTransaction)
                        .getContent();

        return new AdminDashboardResponse(
                userRepository.countByRole(Role.USER),
                userRepository.countByRoleAndStatus(Role.USER, UserStatus.ACTIVE),
                userRepository.countByRoleAndStatus(Role.USER, UserStatus.BLOCKED),
                accountRepository.count(),
                accountRepository.countByStatus(AccountStatus.ACTIVE),
                accountRepository.countByStatus(AccountStatus.BLOCKED),
                accountRepository.countByStatus(AccountStatus.CLOSED),
                accountRepository.countByAccountType(AccountType.SAVINGS),
                accountRepository.countByAccountType(AccountType.CURRENT),
                allTime.getTransactionCount(),
                allTime.getCreditVolume(),
                allTime.getDebitVolume(),
                allTime.getTransferVolume(),
                toSummary(daily),
                toSummary(monthly),
                recentTransactions);
    }

    private AdminActivityProjection activity(
            LocalDate startDate,
            LocalDate endDate) {
        return transactionRepository.getAdminActivity(
                TransactionType.CREDIT,
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                startDate.atStartOfDay(),
                endDate.atStartOfDay());
    }

    private AdminActivitySummary toSummary(AdminActivityProjection projection) {
        return new AdminActivitySummary(
                projection.getTransactionCount(),
                projection.getCreditVolume(),
                projection.getDebitVolume(),
                projection.getTransferVolume());
    }

    private AdminRecentTransactionResponse toRecentTransaction(
            BankTransaction transaction) {
        String customerName = transaction.getAccount().getUser().getFirstName()
                + " " + transaction.getAccount().getUser().getLastName();
        return new AdminRecentTransactionResponse(
                transaction.getTransactionReference(),
                transaction.getAccount().getAccountNumber(),
                customerName,
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getCreatedAt());
    }
}

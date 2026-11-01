package com.smartbank.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartbank.dto.WithdrawalRequest;
import com.smartbank.dto.WithdrawalResponse;
import com.smartbank.entity.Account;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.BankTransaction;
import com.smartbank.entity.IdempotencyRecord;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import com.smartbank.entity.User;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.exception.InvalidTransferException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.BankTransactionRepository;
import com.smartbank.repository.IdempotencyRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WithdrawalExecutionService {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRepository;

    @Transactional
    public WithdrawalResponse execute(
            String accountNumber,
            WithdrawalRequest request,
            String idempotencyKey,
            String requestHash,
            User user) {

        IdempotencyRecord record = new IdempotencyRecord();
        record.setUser(user);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(TransactionStatus.PENDING);
        idempotencyRepository.saveAndFlush(record);

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new InvalidTransferException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new InvalidTransferException(
                    "You are not authorized to use this account");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransferException("Account is not active");
        }

        var amount = request.getAmount();
        if (amount.scale() > 2) {
            throw new InvalidTransferException(
                    "Withdrawal amount cannot have more than 2 decimal places");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        String reference = "WDR-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 20).toUpperCase();
        BankTransaction transaction = BankTransaction.builder()
                .transactionReference(reference)
                .account(account)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Customer withdrawal")
                .build();
        transactionRepository.saveAndFlush(transaction);

        LocalDateTime createdAt = transaction.getCreatedAt() == null
                ? LocalDateTime.now()
                : transaction.getCreatedAt();
        record.setTransactionReference(reference);
        record.setSenderAccountNumber(account.getAccountNumber());
        record.setAmount(amount);
        record.setResponseBalance(account.getBalance());
        record.setStatus(TransactionStatus.SUCCESS);
        record.setDescription("Customer withdrawal");
        record.setResponseCreatedAt(createdAt);
        idempotencyRepository.saveAndFlush(record);

        return new WithdrawalResponse(
                reference,
                account.getAccountNumber(),
                amount,
                account.getBalance(),
                TransactionStatus.SUCCESS,
                createdAt);
    }
}

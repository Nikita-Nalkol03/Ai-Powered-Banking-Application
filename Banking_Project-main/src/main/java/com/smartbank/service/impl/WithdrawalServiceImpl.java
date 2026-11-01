package com.smartbank.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartbank.dto.WithdrawalRequest;
import com.smartbank.dto.WithdrawalResponse;
import com.smartbank.entity.IdempotencyRecord;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.User;
import com.smartbank.exception.InvalidTransferException;
import com.smartbank.repository.IdempotencyRecordRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.WithdrawalService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final IdempotencyRecordRepository idempotencyRepository;
    private final UserRepository userRepository;
    private final WithdrawalExecutionService executionService;

    @Override
    public WithdrawalResponse withdraw(
            String accountNumber,
            WithdrawalRequest request,
            String idempotencyKey) {

        validateRequest(accountNumber, request);
        validateIdempotencyKey(idempotencyKey);

        User user = getAuthenticatedUser();
        String normalizedAccount = accountNumber.trim();
        String normalizedKey = idempotencyKey.trim();
        String requestHash = hashRequest(normalizedAccount, request);

        var existing = idempotencyRepository.findByUserIdAndIdempotencyKey(
                user.getId(), normalizedKey);
        if (existing.isPresent()) {
            return replayOrReject(existing.get(), requestHash);
        }

        try {
            return executionService.execute(
                    normalizedAccount, request, normalizedKey, requestHash, user);
        } catch (DataIntegrityViolationException exception) {
            var concurrent = idempotencyRepository.findByUserIdAndIdempotencyKey(
                    user.getId(), normalizedKey);
            if (concurrent.isPresent()) {
                return replayOrReject(concurrent.get(), requestHash);
            }
            throw exception;
        }
    }

    private WithdrawalResponse replayOrReject(
            IdempotencyRecord record,
            String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency-Key was already used with a different request");
        }
        if (record.getStatus() != TransactionStatus.SUCCESS
                || record.getTransactionReference() == null
                || record.getSenderAccountNumber() == null
                || record.getAmount() == null
                || record.getResponseBalance() == null
                || record.getResponseCreatedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A completed response is not available for this idempotency key");
        }

        return new WithdrawalResponse(
                record.getTransactionReference(),
                record.getSenderAccountNumber(),
                record.getAmount(),
                record.getResponseBalance(),
                record.getStatus(),
                record.getResponseCreatedAt());
    }

    private void validateRequest(String accountNumber, WithdrawalRequest request) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidTransferException("Account number is required");
        }
        if (request == null || request.getAmount() == null
                || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException(
                    "Withdrawal amount must be greater than zero");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key is required and must not exceed 100 characters");
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private String hashRequest(String accountNumber, WithdrawalRequest request) {
        String value = accountNumber + "\n"
                + request.getAmount().stripTrailingZeros().toPlainString();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", exception);
        }
    }
}

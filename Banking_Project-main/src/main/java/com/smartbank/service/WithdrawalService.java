package com.smartbank.service;

import com.smartbank.dto.WithdrawalRequest;
import com.smartbank.dto.WithdrawalResponse;

public interface WithdrawalService {

    WithdrawalResponse withdraw(
            String accountNumber,
            WithdrawalRequest request,
            String idempotencyKey
    );
}

package com.smartbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.smartbank.dto.WithdrawalRequest;
import com.smartbank.dto.WithdrawalResponse;
import com.smartbank.entity.Account;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import com.smartbank.entity.UserStatus;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.exception.InvalidTransferException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.BankTransactionRepository;
import com.smartbank.repository.IdempotencyRecordRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.WithdrawalService;

@SpringBootTest
class WithdrawalIntegrationTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long userId;
    private Long accountId;
    private Long otherUserId;
    private Long otherAccountId;
    private String email;
    private String accountNumber;
    private String otherAccountNumber;

    @BeforeEach
    void setUp() {
        email = "withdraw-" + UUID.randomUUID() + "@smartbank.test";
        accountNumber = generateAccountNumber();
        otherAccountNumber = generateAccountNumber();

        transactionTemplate.executeWithoutResult(status -> {
            User user = createUser(email);
            userRepository.saveAndFlush(user);
            userId = user.getId();

            User otherUser = createUser(
                    "other-" + UUID.randomUUID() + "@smartbank.test");
            userRepository.saveAndFlush(otherUser);
            otherUserId = otherUser.getId();

            Account account = createAccount(accountNumber, user);
            Account otherAccount = createAccount(otherAccountNumber, otherUser);
            accountRepository.saveAndFlush(account);
            accountRepository.saveAndFlush(otherAccount);
            accountId = account.getId();
            otherAccountId = otherAccount.getId();
        });
    }

    @Test
    void withdrawalDebitsAccountAndRetryDoesNotDebitTwice() {
        String key = UUID.randomUUID().toString();
        WithdrawalRequest request = request("125.50");

        WithdrawalResponse first = withdrawAsUser(accountNumber, request, key);
        WithdrawalResponse retry = withdrawAsUser(accountNumber, request, key);

        assertEquals(first.getTransactionReference(), retry.getTransactionReference());
        assertEquals(new BigDecimal("4874.50"), retry.getNewBalance());
        assertEquals(new BigDecimal("4874.50"),
                accountRepository.findById(accountId).orElseThrow().getBalance());
        assertEquals(1, transactionRepository.findByAccountId(
                accountId, org.springframework.data.domain.PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void withdrawalRejectsInsufficientFunds() {
        assertThrows(
                InsufficientBalanceException.class,
                () -> withdrawAsUser(
                        accountNumber, request("6000.00"), UUID.randomUUID().toString()));
    }

    @Test
    void withdrawalRejectsAccountsOwnedByAnotherUser() {
        assertThrows(
                InvalidTransferException.class,
                () -> withdrawAsUser(
                        otherAccountNumber, request("10.00"), UUID.randomUUID().toString()));
    }

    @Test
    void sameKeyWithDifferentAmountIsRejected() {
        String key = UUID.randomUUID().toString();
        withdrawAsUser(accountNumber, request("10.00"), key);

        assertThrows(
                ResponseStatusException.class,
                () -> withdrawAsUser(accountNumber, request("20.00"), key));
    }

    private WithdrawalRequest request(String amount) {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private WithdrawalResponse withdrawAsUser(
            String number, WithdrawalRequest request, String key) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        try {
            return withdrawalService.withdraw(number, request, key);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User createUser(String userEmail) {
        return User.builder()
                .firstName("Withdrawal")
                .lastName("Tester")
                .email(userEmail)
                .phone(generatePhoneNumber())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Account createAccount(String number, User user) {
        return Account.builder()
                .accountNumber(number)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .user(user)
                .build();
    }

    @AfterEach
    void cleanUp() {
        if (userId == null || otherUserId == null) return;

        transactionTemplate.executeWithoutResult(status -> {
            idempotencyRepository.deleteByUserId(userId);
            idempotencyRepository.deleteByUserId(otherUserId);
            transactionRepository.deleteAll(
                    transactionRepository.findAll().stream()
                            .filter(transaction ->
                                    transaction.getAccount().getId().equals(accountId)
                                    || transaction.getAccount().getId().equals(otherAccountId))
                            .toList());
            accountRepository.deleteAllById(List.of(accountId, otherAccountId));
            userRepository.deleteAllById(List.of(userId, otherUserId));
        });
    }

    private String generateAccountNumber() {
        return Long.toString(
                10_000_000_000L + Math.floorMod(
                        UUID.randomUUID().getMostSignificantBits(), 89_999_999_999L));
    }

    private String generatePhoneNumber() {
        return Long.toString(
                100_000_000_000_000L + Math.floorMod(
                        UUID.randomUUID().getLeastSignificantBits(), 899_999_999_999_999L));
    }
}

package com.smartbank;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.smartbank.entity.Account;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.BankTransaction;
import com.smartbank.entity.Role;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import com.smartbank.entity.User;
import com.smartbank.entity.UserStatus;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.BankTransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User admin;
    private User customer;
    private Account account;
    private BankTransaction transferCredit;
    private String adminToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        transactionTemplate.executeWithoutResult(transaction -> {
            admin = userRepository.save(User.builder()
                    .firstName("Dashboard")
                    .lastName("Admin")
                    .email("dashboard-admin-" + suffix + "@example.com")
                    .phone("8" + suffix.substring(0, 9))
                    .password(passwordEncoder.encode("TestPassword123!"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build());
            customer = userRepository.save(User.builder()
                    .firstName("Dashboard")
                    .lastName("Customer")
                    .email("dashboard-customer-" + suffix + "@example.com")
                    .phone("9" + suffix.substring(0, 9))
                    .password(passwordEncoder.encode("TestPassword123!"))
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .build());
            account = accountRepository.save(Account.builder()
                    .accountNumber("AD" + suffix.substring(0, 18))
                    .accountType(AccountType.SAVINGS)
                    .balance(new BigDecimal("12.34"))
                    .status(AccountStatus.ACTIVE)
                    .user(customer)
                    .build());
            transferCredit = transactionRepository.save(BankTransaction.builder()
                    .transactionReference("TXN-" + suffix + "-C")
                    .account(account)
                    .type(TransactionType.CREDIT)
                    .amount(new BigDecimal("12.34"))
                    .status(TransactionStatus.SUCCESS)
                    .description("Dashboard integration test")
                    .build());
        });

        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(admin.getEmail())
                .password("not-used")
                .authorities("ROLE_ADMIN")
                .build();
        adminToken = jwtService.generateAccessToken(principal);
    }

    @AfterEach
    void cleanUp() {
        if (admin == null || customer == null || account == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(transaction -> {
            transactionRepository.delete(transferCredit);
            accountRepository.delete(account);
            userRepository.delete(customer);
            userRepository.delete(admin);
        });
    }

    @Test
    void adminDashboardReturnsDatabaseMetricsAndRecentLedgerData()
            throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCustomers")
                        .value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeAccounts")
                        .value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalTransferVolume")
                        .value(greaterThanOrEqualTo(12.34)))
                .andExpect(jsonPath("$.dailyActivity.transferVolume")
                        .value(greaterThanOrEqualTo(12.34)))
                .andExpect(jsonPath("$.recentTransactions[0].transactionReference")
                        .value(transferCredit.getTransactionReference()))
                .andExpect(jsonPath("$.recentTransactions[0].customerName")
                        .value("Dashboard Customer"))
                .andExpect(jsonPath("$.recentTransactions[0].password").doesNotExist());
    }
}

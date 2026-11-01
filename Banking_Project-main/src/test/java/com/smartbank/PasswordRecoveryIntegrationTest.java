package com.smartbank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.smartbank.repository.RefreshTokenRecordRepository;
import com.smartbank.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordRecoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRecordRepository refreshTokenRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final String email = "recovery-" + UUID.randomUUID() + "@smartbank.test";

    @BeforeEach
    void registerUser() throws Exception {
        String phoneDigits = String.format(
                "%09d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1_000_000_000L));
        String phone = "9" + phoneDigits;
        Map<String, String> registration = Map.of(
                "firstName", "Recovery",
                "lastName", "Tester",
                "email", email,
                "phone", phone,
                "password", "InitialPass123!",
                "securityQuestion", "FIRST_SCHOOL",
                "securityAnswer", "Green Valley");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());
    }

    @Test
    void recoveryQuestionIsLoadedForTheEnteredEmail() throws Exception {
        mockMvc.perform(post("/api/auth/recovery-question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityQuestion").value("FIRST_SCHOOL"));
    }

    @Test
    void correctAnswerResetsPasswordAndOldPasswordNoLongerWorks() throws Exception {
        Map<String, String> recovery = Map.of(
                "email", email,
                "securityQuestion", "FIRST_SCHOOL",
                "securityAnswer", "  GREEN   VALLEY ",
                "newPassword", "NewPass456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recovery)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "InitialPass123!"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "NewPass456!"))))
                .andExpect(status().isOk());
    }

    @Test
    void wrongAnswerIsRejectedAndRateLimited() throws Exception {
        Map<String, String> wrongAnswer = Map.of(
                "email", email,
                "securityQuestion", "FIRST_SCHOOL",
                "securityAnswer", "Wrong Answer",
                "newPassword", "NewPass456!");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongAnswer)))
                    .andExpect(status().isBadRequest());
        }

        Map<String, String> correctAnswer = Map.of(
                "email", email,
                "securityQuestion", "FIRST_SCHOOL",
                "securityAnswer", "Green Valley",
                "newPassword", "NewPass456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctAnswer)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "InitialPass123!"))))
                .andExpect(status().isOk());
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(email).ifPresent(user -> {
            refreshTokenRepository.deleteAll(
                    refreshTokenRepository.findByUserId(user.getId()));
            userRepository.delete(user);
        });
    }
}

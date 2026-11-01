
package com.smartbank.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartbank.dto.LoginRequest;
import com.smartbank.dto.LoginResponse;
import com.smartbank.dto.RefreshTokenRequest;
import com.smartbank.dto.RegisterRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.dto.PasswordResetRequest;
import com.smartbank.dto.RecoveryQuestionRequest;
import com.smartbank.dto.RecoveryQuestionResponse;
import com.smartbank.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {

        if (!authService.resetPassword(request)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Could not reset the password. Check your details or try again later."));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recovery-question")
    public ResponseEntity<RecoveryQuestionResponse> getRecoveryQuestion(
            @Valid @RequestBody RecoveryQuestionRequest request) {

        return ResponseEntity.ok(authService.getRecoveryQuestion(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
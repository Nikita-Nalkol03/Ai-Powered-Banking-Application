
package com.smartbank.service;

import com.smartbank.dto.LoginRequest;
import com.smartbank.dto.LoginResponse;
import com.smartbank.dto.RefreshTokenRequest;
import com.smartbank.dto.RegisterRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.dto.PasswordResetRequest;
import com.smartbank.dto.RecoveryQuestionRequest;
import com.smartbank.dto.RecoveryQuestionResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    boolean resetPassword(PasswordResetRequest request);

    RecoveryQuestionResponse getRecoveryQuestion(RecoveryQuestionRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
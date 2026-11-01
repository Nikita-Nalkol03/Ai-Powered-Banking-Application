package com.smartbank.dto;

import com.smartbank.entity.SecurityQuestion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Security question is required")
    private SecurityQuestion securityQuestion;

    @NotBlank(message = "Security answer is required")
    @Size(min = 2, max = 100, message = "Security answer must be between 2 and 100 characters")
    private String securityAnswer;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String newPassword;
}

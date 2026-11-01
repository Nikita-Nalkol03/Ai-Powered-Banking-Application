package com.smartbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpendingInsightRequest {

    @NotBlank(message = "Account number is required")
    @Size(max = 20, message = "Account number is invalid")
    private String accountNumber;
}

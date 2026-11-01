package com.smartbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 1000, message = "Question cannot exceed 1000 characters")
    private String message;
}

package com.smartbank.dto;

import com.smartbank.entity.SecurityQuestion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecoveryQuestionResponse {

    private SecurityQuestion securityQuestion;
}

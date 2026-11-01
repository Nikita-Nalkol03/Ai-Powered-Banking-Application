package com.smartbank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartbank.dto.AiAnswerResponse;
import com.smartbank.dto.AiQuestionRequest;
import com.smartbank.dto.MonthlySpendingInsightResponse;
import com.smartbank.dto.SpendingInsightRequest;
import com.smartbank.service.AiAssistantService;
import com.smartbank.service.SpendingInsightService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@Validated
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final SpendingInsightService spendingInsightService;

    @PostMapping("/chat")
    public ResponseEntity<AiAnswerResponse> chat(
            @Valid @RequestBody AiQuestionRequest request) {

        return ResponseEntity.ok(
                new AiAnswerResponse(aiAssistantService.answer(request.getMessage())));
    }

    @PostMapping("/spending-insights")
    public ResponseEntity<MonthlySpendingInsightResponse> spendingInsights(
            @Valid @RequestBody SpendingInsightRequest request) {

        return ResponseEntity.ok(
                spendingInsightService.analyzeCurrentMonth(request.getAccountNumber()));
    }
}

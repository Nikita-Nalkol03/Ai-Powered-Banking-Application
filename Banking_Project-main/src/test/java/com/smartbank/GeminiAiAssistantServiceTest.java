package com.smartbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.smartbank.service.impl.GeminiAiAssistantService;

class GeminiAiAssistantServiceTest {

    @Test
    void missingApiKeyReturnsClearServiceUnavailableError() {
        GeminiAiAssistantService service = new GeminiAiAssistantService(
                "", "gemini-2.5-flash");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.answer("How do I make a budget?"));

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode());
        assertTrue(exception.getReason().contains("GEMINI_API_KEY"));
    }
}

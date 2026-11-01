package com.smartbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.smartbank.service.impl.GeminiAiAssistantService;

class GeminiApiIntegrationTest {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiApiIntegrationTest.class);
    private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";

    @Test
    void configuredGeminiApiKeyAndModelReachGoogle() {
        if (!"true".equalsIgnoreCase(System.getenv("GEMINI_INTEGRATION_TEST"))) {
            log.info("Real Gemini integration test not executed: "
                    + "set GEMINI_INTEGRATION_TEST=true to enable it.");
            assumeTrue(false, "GEMINI_INTEGRATION_TEST is not enabled");
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Real Gemini integration test not executed: "
                    + "GEMINI_API_KEY is not configured.");
            assumeTrue(false, "GEMINI_API_KEY is not configured");
        }

        String configuredModel = System.getenv("GEMINI_MODEL");
        String model = configuredModel == null || configuredModel.isBlank()
                ? DEFAULT_MODEL
                : configuredModel.trim();
        GeminiAiAssistantService service =
                new GeminiAiAssistantService(apiKey, model);

        int status;
        try {
            status = service.checkConnectivity();
        } catch (ResponseStatusException exception) {
            String reason = exception.getReason() == null
                    ? "No provider details were returned."
                    : exception.getReason();
            if (reason.startsWith("Could not connect to the AI assistant")) {
                log.warn("Real Gemini integration test not executed: "
                        + "Google's API could not be reached.");
                assumeTrue(false, "External Gemini API access is unavailable");
            }
            log.error("Real Gemini connectivity failed: {}", reason);
            throw exception;
        }

        log.info("Real Gemini connectivity succeeded: HTTP {} using model {}",
                status, model);
        assertEquals(HttpStatus.OK.value(), status);
    }
}

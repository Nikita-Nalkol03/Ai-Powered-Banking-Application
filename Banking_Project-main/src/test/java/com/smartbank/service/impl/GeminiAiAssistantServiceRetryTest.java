package com.smartbank.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class GeminiAiAssistantServiceRetryTest {

    private static final String API_KEY = "unit-test-secret";
    private static final String MODEL = "gemini-3.5-flash-lite";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent";

    @Test
    void retriesTransientServerFailureAndThenReturnsAnswer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"temporary outage\"}}"));
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Connected"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GeminiAiAssistantService service =
                new GeminiAiAssistantService(API_KEY, MODEL, builder.build());

        assertEquals("Connected", service.answer("Test"));
        server.verify();
    }

    @Test
    void connectivityCheckUsesMinimalGenerateContentRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(content().json("""
                        {
                          "contents": [
                            {"parts": [{"text": "Reply with exactly: OK"}]}
                          ],
                          "generationConfig": {
                            "temperature": 0.0,
                            "maxOutputTokens": 8
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GeminiAiAssistantService service =
                new GeminiAiAssistantService(API_KEY, MODEL, builder.build());

        assertEquals(HttpStatus.OK.value(), service.checkConnectivity());
        server.verify();
    }

    @Test
    void invalidModelRequestIsNotRetriedAndProviderMessageIsSanitized() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Bad request: "
                                + API_KEY + "\"}}"));

        GeminiAiAssistantService service =
                new GeminiAiAssistantService(API_KEY, MODEL, builder.build());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.answer("Test"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("HTTP 400"));
        assertTrue(exception.getReason().contains("Google reported"));
        assertFalse(exception.getReason().contains(API_KEY));
        server.verify();
    }

    @Test
    void authenticationAndRateLimitFailuresHaveDistinctMessages() {
        assertFailureMessage(
                HttpStatus.FORBIDDEN,
                "API key does not have access",
                "API key or its permissions",
                HttpStatus.SERVICE_UNAVAILABLE);
        assertFailureMessage(
                HttpStatus.NOT_FOUND,
                "Model not found",
                "model or request",
                HttpStatus.BAD_GATEWAY);
        assertFailureMessage(
                HttpStatus.TOO_MANY_REQUESTS,
                "Quota exceeded",
                "rate limit or quota",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void assertFailureMessage(
            HttpStatus providerStatus,
            String providerMessage,
            String expectedReason,
            HttpStatus expectedApplicationStatus) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(providerStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\""
                                + providerMessage + "\"}}"));

        GeminiAiAssistantService service =
                new GeminiAiAssistantService(API_KEY, MODEL, builder.build());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.answer("Test"));

        assertEquals(expectedApplicationStatus, exception.getStatusCode());
        assertTrue(exception.getReason().contains(expectedReason));
        assertTrue(exception.getReason().contains(providerMessage));
        server.verify();
    }
}

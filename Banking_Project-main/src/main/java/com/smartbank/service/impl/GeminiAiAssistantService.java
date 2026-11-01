package com.smartbank.service.impl;

import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.time.Duration;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.smartbank.service.AiAssistantService;

@Service
public class GeminiAiAssistantService implements AiAssistantService {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiAiAssistantService.class);
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 250;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 300;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final String SYSTEM_INSTRUCTION = """
            You are ArthVeda's general banking education assistant.
            Answer clearly and briefly in plain language.
            Give general educational information only, not personalized financial,
            investment, tax, or legal advice. Do not claim to access a customer's
            account, transactions, balance, or private information. Do not request
            passwords, PINs, card numbers, one-time codes, or security answers.
            You may analyze aggregate monthly credit and debit totals explicitly
            included in a spending-analysis prompt. Do not infer merchant,
            category, or transaction details that are not provided. If the user
            asks for account-specific information that is not in the prompt,
            explain that you cannot see it and direct them to their secure
            ArthVeda dashboard.
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    @Autowired
    public GeminiAiAssistantService(
            @Value("${smartbank.ai.api-key:}") String apiKey,
            @Value("${smartbank.ai.model:gemini-2.5-flash}") String model) {
        this(apiKey, model, createRestClient());
    }

    GeminiAiAssistantService(String apiKey, String model, RestClient restClient) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    private static RestClient createRestClient() {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @PostConstruct
    void logConfigurationStatus() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not configured; AI requests will return service unavailable.");
        } else {
            log.info("ArthVeda AI is configured with model {}", model);
        }
    }

    @Override
    public String answer(String question) {
        ResponseEntity<JsonNode> response =
                generateContent(question.trim(), true, 500, 0.4);
        String answer = extractAnswer(response.getBody());
        if (answer.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI assistant returned an empty answer. Please try again.");
        }
        return answer;
    }

    public int checkConnectivity() {
        ResponseEntity<JsonNode> response = generateContent(
                "Reply with exactly: OK",
                false,
                8,
                0);
        if (extractAnswer(response.getBody()).isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini connectivity request succeeded but returned no text.");
        }
        return response.getStatusCode().value();
    }

    private ResponseEntity<JsonNode> generateContent(
            String prompt,
            boolean includeSystemInstruction,
            int maxOutputTokens,
            double temperature) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI assistant is not configured. Set GEMINI_API_KEY for the backend.");
        }

        Map<String, Object> requestBody = requestBody(
                prompt, includeSystemInstruction, maxOutputTokens, temperature);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.post()
                        .uri(GEMINI_API_URL + model + ":generateContent")
                        .header("x-goog-api-key", apiKey)
                        .body(requestBody)
                        .retrieve()
                        .toEntity(JsonNode.class);
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                String providerMessage = sanitizedGoogleMessage(exception);
                if (isTransientStatus(status) && attempt < MAX_ATTEMPTS) {
                    log.warn(
                            "Transient Gemini failure (HTTP {}); retrying attempt {}/{}: {}",
                            status, attempt + 1, MAX_ATTEMPTS, providerMessage);
                    pauseBeforeRetry(attempt);
                    continue;
                }

                log.warn(
                        "Gemini request failed with HTTP status {}: {}",
                        status, providerMessage);
                throw providerException(status, providerMessage);
            } catch (ResourceAccessException exception) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn(
                            "Transient network error reaching Gemini; retrying attempt {}/{}",
                            attempt + 1, MAX_ATTEMPTS);
                    pauseBeforeRetry(attempt);
                    continue;
                }
                log.warn("Could not reach the Gemini service after {} attempts", attempt);
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not connect to the AI assistant after 3 attempts. Please try again later.");
            } catch (RestClientException exception) {
                log.warn("Gemini request could not be processed by the HTTP client");
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not process the AI assistant request. Please try again later.");
            }
        }

        throw new IllegalStateException("Gemini retry loop ended unexpectedly");
    }

    private Map<String, Object> requestBody(
            String prompt,
            boolean includeSystemInstruction,
            int maxOutputTokens,
            double temperature) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens));
        if (!includeSystemInstruction) {
            return body;
        }

        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
                "contents", body.get("contents"),
                "generationConfig", body.get("generationConfig"));
    }

    private boolean isTransientStatus(int status) {
        return status >= 500 && status <= 599;
    }

    private void pauseBeforeRetry(int attempt) {
        long delay = INITIAL_RETRY_DELAY_MILLIS * (1L << (attempt - 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI assistant request was interrupted while retrying.");
        }
    }

    private ResponseStatusException providerException(
            int status,
            String providerMessage) {
        String details = providerMessage.isBlank()
                ? ""
                : " Google reported: " + providerMessage;
        if (status == 401 || status == 403) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini rejected the configured API key or its permissions (HTTP "
                            + status + "). Check GEMINI_API_KEY and key restrictions." + details);
        }
        if (status == 400 || status == 404) {
            return new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini rejected the configured model or request (HTTP "
                            + status + "). Check GEMINI_MODEL and the request configuration."
                            + details);
        }
        if (status == 429) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini rate limit or quota was reached (HTTP 429). Check provider quota and try again later."
                            + details);
        }
        if (isTransientStatus(status)) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini remained unavailable after " + MAX_ATTEMPTS
                            + " attempts (HTTP " + status + "). Please try again later."
                            + details);
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Gemini rejected the request (HTTP " + status + ")." + details);
    }

    private String sanitizedGoogleMessage(RestClientResponseException exception) {
        String message;
        try {
            JsonNode response = JSON_MAPPER.readTree(
                    exception.getResponseBodyAsString());
            if (response == null) {
                return "";
            }
            JsonNode error = response.path("error").path("message");
            message = error.asText("");
        } catch (JacksonException ignored) {
            return "";
        }

        if (apiKey != null && !apiKey.isBlank()) {
            message = message.replace(apiKey, "[redacted]");
        }
        message = message.replaceAll("(?i)AIza[0-9A-Za-z_-]{20,}", "[redacted]")
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
        }
        return message;
    }

    private String extractAnswer(JsonNode response) {
        if (response == null) {
            return "";
        }

        JsonNode parts = response.path("candidates")
                .path(0)
                .path("content")
                .path("parts");
        if (!parts.isArray()) {
            return "";
        }

        StringBuilder answer = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                if (!answer.isEmpty()) {
                    answer.append('\n');
                }
                answer.append(text);
            }
        }
        return answer.toString().trim();
    }
}

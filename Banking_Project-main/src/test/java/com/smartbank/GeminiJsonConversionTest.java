package com.smartbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

class GeminiJsonConversionTest {

    @Test
    void restClientReadsGeminiResponseAsJacksonJsonNode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://gemini.test/generate"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [{"text": "Your answer"}]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = builder.build()
                .post()
                .uri("https://gemini.test/generate")
                .retrieve()
                .body(JsonNode.class);

        assertEquals(
                "Your answer",
                response.path("candidates").path(0)
                        .path("content").path("parts").path(0)
                        .path("text").asText());
        server.verify();
    }
}

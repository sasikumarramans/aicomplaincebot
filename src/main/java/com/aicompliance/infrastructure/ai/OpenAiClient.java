package com.aicompliance.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Thin wrapper around OpenAI's chat completions endpoint. Kept deliberately small (one method,
 * JSON-mode only) so each AiService use case's prompt/response contract stays explicit in
 * OpenAiService rather than hidden behind a generic "call the model" abstraction.
 */
@Component
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public OpenAiClient(WebClient openAiWebClient, ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls chat completions in JSON-object mode and returns the parsed response content as a
     * JsonNode. The system/user prompts must themselves instruct the model to reply with JSON
     * matching the expected shape - OpenAI's json_object mode only guarantees valid JSON syntax,
     * not a particular schema.
     */
    public JsonNode completeAsJson(String model, String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        JsonNode response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new OpenAiRequestException("OpenAI returned no response body");
        }

        String content = response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null) {
            throw new OpenAiRequestException("OpenAI response did not contain message content: " + response);
        }

        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new OpenAiRequestException("OpenAI response content was not valid JSON: " + content, e);
        }
    }
}

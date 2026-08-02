package com.aicompliance.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Explicitly wires the classic-Jackson {@link ObjectMapper} (see {@link JacksonConfig}) into
     * this WebClient's codecs. Without this, WebClient falls back to Boot 4.1's default Jackson 3
     * decoder for {@code bodyToMono(JsonNode.class)} calls in {@code OpenAiClient} - and since that
     * call requests the classic {@code com.fasterxml.jackson.databind.JsonNode} type, the Jackson 3
     * decoder can't construct it (no creators for an abstract type from a different Jackson major
     * version), failing every OpenAI call with an InvalidDefinitionException.
     */
    @Bean
    public WebClient openAiWebClient(AppProperties appProperties, ObjectMapper objectMapper) {
        AppProperties.OpenAi openAi = appProperties.getOpenai();
        return WebClient.builder()
                .baseUrl(openAi.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAi.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();
    }
}

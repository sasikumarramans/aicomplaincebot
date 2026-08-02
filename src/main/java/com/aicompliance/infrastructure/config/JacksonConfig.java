package com.aicompliance.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4.1's auto-configured ObjectMapper is Jackson 3 ({@code tools.jackson.databind}).
 * This codebase uses classic Jackson 2 ({@code com.fasterxml.jackson.databind}) for JSON
 * handling outside the web layer (e.g. parsing OpenAI responses, serializing extracted fields),
 * since Jackson 2 is what AWS SDK v2 and most third-party libraries still integrate with. This
 * bean provides that classic ObjectMapper explicitly, since Boot no longer registers one.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}

package com.today.fridge.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fastapi.llm")
public record LlmClientProperties(
        String baseUrl
) {
}
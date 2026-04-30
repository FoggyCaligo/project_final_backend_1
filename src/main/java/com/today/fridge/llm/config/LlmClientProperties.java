package com.today.fridge.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fastapi")
public record LlmClientProperties(
        String baseUrl
) {
}
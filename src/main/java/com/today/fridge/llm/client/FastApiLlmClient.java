package com.today.fridge.llm.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.today.fridge.llm.config.LlmClientProperties;
import com.today.fridge.llm.dto.request.RecommendationExplainRequest;
import com.today.fridge.llm.dto.response.RecommendationExplainResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FastApiLlmClient {

    private final RestClient.Builder restClientBuilder;
    private final LlmClientProperties properties;

    public RecommendationExplainResponse explainRecommendation(RecommendationExplainRequest request) {
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .build()
                .post()
                .uri("/internal/v1/llm/recommendation/explain")
                .body(request)
                .retrieve()
                .body(RecommendationExplainResponse.class);
    }
}
package com.today.fridge.llm.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.today.fridge.llm.config.LlmClientProperties;
import com.today.fridge.llm.dto.request.RecommendationExplanationContext;
import com.today.fridge.llm.dto.request.SubstitutionLlmRequest;
import com.today.fridge.llm.dto.response.RecommendationExplainResponse;
import com.today.fridge.llm.dto.response.SubstitutionLlmResponse;

import com.today.fridge.meal.dto.request.FastApiHealthReportRequest;
import com.today.fridge.meal.dto.response.FastApiHealthReportResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FastApiLlmClient {

    private final RestClient.Builder restClientBuilder;
    private final LlmClientProperties properties;

    public RecommendationExplainResponse explainRecommendation(
            RecommendationExplanationContext context
    ) {
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .build()
                .post()
                .uri("/api/v1/internal/llm/recommendation/explain")
                .body(context)
                .retrieve()
                .body(RecommendationExplainResponse.class);
    }
    
    public SubstitutionLlmResponse suggestSubstitutions(
            SubstitutionLlmRequest request
    ) {
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .build()
                .post()
                .uri("/api/v1/internal/llm/substitutions/suggest")
                .body(request)
                .retrieve()
                .body(SubstitutionLlmResponse.class);
    }

    public FastApiHealthReportResponse generateHealthReport(
            FastApiHealthReportRequest request
    ) {
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .build()
                .post()
                .uri("/api/v1/health-report/generate")
                .body(request)
                .retrieve()
                .body(FastApiHealthReportResponse.class);
    }
}
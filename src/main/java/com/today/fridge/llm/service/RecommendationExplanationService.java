package com.today.fridge.llm.service;

import org.springframework.stereotype.Service;

import com.today.fridge.llm.client.FastApiLlmClient;
import com.today.fridge.llm.dto.request.RecommendationExplanationContext;
import com.today.fridge.llm.dto.response.RecommendationExplainResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationExplanationService {

    private final FastApiLlmClient fastApiLlmClient;

    public String generateExplanation(RecommendationExplanationContext context) {
        RecommendationExplainResponse response =
                fastApiLlmClient.explainRecommendation(context);

        return response.explanation();
    }
}
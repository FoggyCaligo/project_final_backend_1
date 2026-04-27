package com.today.fridge.chatbot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.chatbot.dto.request.ChatInterpretRequest;
import com.today.fridge.chatbot.dto.response.ChatInterpretResponse;
import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatbotOrchestratorService {

    private final IntentParserService intentParserService;
    private final RecommendationService recommendationService;

    public List<RecipeRecommendationResponse> recommendFromChat(
            ChatInterpretRequest request
    ) {

        ChatInterpretResponse parsed =
                intentParserService.interpret(request);

        RecommendationQuery query =
                RecommendationQuery.builder()
                        .userId(null) // 비회원 고려
                        .conditionCodes(parsed.getConditionTags())
                        .includeIngredients(parsed.getIncludeIngredients())
                        .excludeIngredients(parsed.getExcludeIngredients())
                        .keywords(parsed.getKeywords())
                        .sortHint(parsed.getSortHint())
                        .source("CHATBOT")
                        .useUserProfile(false)
                        .useUserFridge(false)
                        .build();

        return recommendationService.recommend(query);
    }
}
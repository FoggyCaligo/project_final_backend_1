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

        Long userId = request.getUserId();
        boolean isMember = userId != null;

        List<String> includeIngredients = parsed.getIncludeIngredients();

        if (isMember && includeIngredients.isEmpty()) {
            // TODO: 회원 냉장고 재료 조회 로직으로 교체
            includeIngredients = List.of("두부", "계란");
        }

        RecommendationQuery query =
                RecommendationQuery.builder()
                        .userId(userId)
                        .conditionCodes(parsed.getConditionTags())
                        .includeIngredients(includeIngredients)
                        .excludeIngredients(parsed.getExcludeIngredients())
                        .keywords(parsed.getKeywords())
                        .sortHint(parsed.getSortHint())
                        .source("CHATBOT")
                        .useUserProfile(isMember)
                        .useUserFridge(isMember)
                        .build();

        return recommendationService.recommend(query);
    }
}
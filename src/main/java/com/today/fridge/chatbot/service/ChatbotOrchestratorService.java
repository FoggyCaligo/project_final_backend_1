package com.today.fridge.chatbot.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.today.fridge.chatbot.dto.request.ChatInterpretRequest;
import com.today.fridge.chatbot.dto.response.ChatInterpretResponse;
import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.recommendation.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatbotOrchestratorService {

    private final IntentParserService intentParserService;
    private final RecommendationService recommendationService;
    private final UserConditionRepository userConditionRepository;

    public List<RecipeRecommendationResponse> recommendFromChat(
            ChatInterpretRequest request
    ) {

        ChatInterpretResponse parsed =
                intentParserService.interpret(request);

        Long userId = request.getUserId();
        boolean isMember = userId != null;
        System.out.println("[CHAT_USER] userId=" + userId + ", isMember=" + isMember);
        List<String> includeIngredients =
                parsed.getIncludeIngredients() == null ? List.of() : parsed.getIncludeIngredients();

        List<String> conditionCodes =
                parsed.getConditionTags() == null ? List.of() : parsed.getConditionTags();
        
        List<String> keywords = new java.util.ArrayList<>();

        keywords.add(request.getText());

        if (parsed.getKeywords() != null) {
            keywords.addAll(parsed.getKeywords());
        }
        
        if (isMember && includeIngredients.isEmpty()) {
            includeIngredients = List.of();
        }
        
        List<String> profileConditionCodes = isMember
                ? userConditionRepository
                        .findByUser_UserIdAndIsActiveTrue(userId)
                        .stream()
                        .map(uc -> uc.getConditionCode().getConditionCode())
                        .toList()
                : List.of();

        conditionCodes = java.util.stream.Stream
                .concat(conditionCodes.stream(), profileConditionCodes.stream())
                .distinct()
                .toList();
        System.out.println("[CHAT_PARSED] conditions=" + conditionCodes);
        System.out.println("[CHAT_PARSED] includeIngredients=" + includeIngredients);
        System.out.println("[CHAT_PARSED] keywords=" + keywords);
        RecommendationQuery query =
                RecommendationQuery.builder()
                        .userId(userId)
                        .conditionCodes(conditionCodes)
                        .includeIngredients(includeIngredients)
                        .excludeIngredients(parsed.getExcludeIngredients())
                        .keywords(keywords)
                        .sortHint(parsed.getSortHint())
                        .source("CHATBOT")
                        .useUserProfile(isMember)
                        .useUserFridge(isMember)
                        .build();

        return recommendationService.recommend(query, Pageable.unpaged()).content().stream().limit(3).toList();
    }
}
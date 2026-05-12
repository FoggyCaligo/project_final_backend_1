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
            Long userId,
            ChatInterpretRequest request
    ) {
        ChatInterpretResponse parsed =
                intentParserService.interpret(request);

        boolean isMember = userId != null;

        List<String> includeIngredients =
                parsed.getIncludeIngredients() == null
                        ? List.of()
                        : parsed.getIncludeIngredients();

        List<String> conditionCodes =
                parsed.getConditionTags() == null
                        ? List.of()
                        : parsed.getConditionTags();

        List<String> excludeIngredients =
                parsed.getExcludeIngredients() == null
                        ? List.of()
                        : parsed.getExcludeIngredients();

        List<String> keywords = new java.util.ArrayList<>();

        if (request.getText() != null && !request.getText().isBlank()) {
            keywords.add(request.getText());
        }

        if (parsed.getKeywords() != null) {
            keywords.addAll(
                    parsed.getKeywords().stream()
                            .filter(k -> k != null && !k.isBlank())
                            .toList()
            );
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
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();

        RecommendationQuery query =
                RecommendationQuery.builder()
                        .userId(userId)
                        .conditionCodes(conditionCodes)
                        .includeIngredients(includeIngredients)
                        .excludeIngredients(excludeIngredients)
                        .keywords(keywords)
                        .sortHint(parsed.getSortHint())
                        .source("CHATBOT")
                        .useUserProfile(isMember)
                        .useUserFridge(isMember)
                        .build();

        return recommendationService
                .recommend(query, Pageable.unpaged())
                .content()
                .stream()
                .limit(3)
                .toList();
    }
}
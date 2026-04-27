package com.today.fridge.chatbot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.chatbot.dto.request.ChatInterpretRequest;
import com.today.fridge.chatbot.dto.response.ChatInterpretResponse;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntentParserService {

    private final ConditionCodeRepository conditionCodeRepository;

    public ChatInterpretResponse interpret(ChatInterpretRequest request) {

        String text = request.getText();

        List<String> conditionTags = extractConditionTags(text);

        List<String> includeIngredients = extractIncludeIngredients(
                text,
                request.getOwnedIngredients()
        );

        List<String> keywords = extractKeywords(text);

        return ChatInterpretResponse.builder()
                .intent(resolveIntent(text))
                .conditionTags(conditionTags)
                .includeIngredients(includeIngredients)
                .excludeIngredients(List.of())
                .keywords(keywords)
                .sortHint("ingredient_match")
                .needsClarification(false)
                .reasonHints(buildReasonHints(conditionTags, includeIngredients))
                .parsedQuerySummary("자연어 요청을 추천 조건으로 구조화했습니다.")
                .confidence(0.8)
                .build();
    }

    private List<String> extractConditionTags(String text) {
        return conditionCodeRepository.findByIsActiveTrue()
                .stream()
                .filter(condition -> containsCondition(text, condition))
                .map(ConditionCode::getConditionCode)
                .distinct()
                .toList();
    }

    private boolean containsCondition(String text, ConditionCode condition) {
        return text.contains(condition.getConditionName())
                || text.contains(condition.getConditionCode());
    }

    private List<String> extractIncludeIngredients(
            String text,
            List<String> ownedIngredients
    ) {
        if (ownedIngredients == null || ownedIngredients.isEmpty()) {
            return List.of();
        }

        return ownedIngredients.stream()
                .filter(text::contains)
                .distinct()
                .toList();
    }

    private List<String> extractKeywords(String text) {
        return List.of("간단", "빠른", "든든", "저녁", "아침", "점심")
                .stream()
                .filter(text::contains)
                .toList();
    }

    private String resolveIntent(String text) {
        if (text.contains("검색")) {
            return "search";
        }

        return "recommend";
    }

    private List<String> buildReasonHints(
            List<String> conditionTags,
            List<String> includeIngredients
    ) {
        if (!conditionTags.isEmpty() && !includeIngredients.isEmpty()) {
            return List.of("사용자 조건과 보유 재료를 함께 고려");
        }

        if (!conditionTags.isEmpty()) {
            return List.of("사용자 조건을 우선 고려");
        }

        if (!includeIngredients.isEmpty()) {
            return List.of("보유 재료를 우선 고려");
        }

        return List.of();
    }
}
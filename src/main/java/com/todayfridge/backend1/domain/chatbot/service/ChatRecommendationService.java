package com.todayfridge.backend1.domain.chatbot.service;

import com.todayfridge.backend1.domain.recipe.service.RecommendationService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatRecommendationService {
    private final ChatInterpretFacade chatInterpretFacade;
    private final RecommendationService recommendationService;

    public ChatRecommendationService(ChatInterpretFacade chatInterpretFacade, RecommendationService recommendationService) {
        this.chatInterpretFacade = chatInterpretFacade;
        this.recommendationService = recommendationService;
    }

    public Map<String, Object> recommend(Long userId, String message) {
        var interpreted = chatInterpretFacade.interpret(message);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("intentType", interpreted.intentType());
        data.put("ingredients", interpreted.ingredients());
        data.put("filters", interpreted.filters());
        data.put("excludedIngredients", interpreted.excludedIngredients());
        data.put("recommendations", recommendationService.recommend(userId).stream().limit(10).toList());
        return data;
    }
}

package com.today.fridge.llm.client;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeTagClient {

    private final RestClient restClient;

    public RecipeTagClassifyResponse classify(
            RecipeTagClassifyRequest request
    ) {
        return restClient.post()
                .uri("http://192.168.0.6:8000/api/v1/internal/recipe-tags/classify")
                .body(request)
                .retrieve()
                .body(RecipeTagClassifyResponse.class);
    }

    // DTO 내부 정의 (간단히)
    public record RecipeTagClassifyRequest(
            Long recipeId,
            String title,
            List<String> ingredients,
            String summary
    ) {}

    public record RecipeTagClassifyResponse(
            Long recipeId,
            List<RecipeTagDto> tags
    ) {}

    public record RecipeTagDto(
            String tagType,
            String tagCode,
            double confidence,
            String sourceType
    ) {}
}
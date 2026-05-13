package com.today.fridge.shopping.external.ai;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * FastAPI LLM 서버에 쇼핑 추천 이유 설명을 요청하는 클라이언트.
 * 실패 시 null을 반환하여 메인 쇼핑 조회 흐름을 차단하지 않습니다.
 */
@Slf4j
@Component
public class ShoppingExplainClient {

    private final RestTemplate restTemplate;
    private final String explainUrl;

    public ShoppingExplainClient(
            RestTemplateBuilder builder,
            @Value("${ai.server.url:http://localhost:8000}") String aiServerUrl) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(10))
                .build();
        this.explainUrl = aiServerUrl + "/api/v1/internal/llm/shopping/explain";
    }

    /**
     * @param ingredientName 식재료명
     * @param items 쇼핑 아이템 목록
     * @param recipeTitle 레시피 제목 (없으면 null)
     * @param matchedIngredients 보유 재료 목록 (없으면 빈 리스트)
     * @param missingIngredients 부족 재료 목록 (없으면 빈 리스트)
     * @return AI 생성 추천 이유 문장, 실패 시 null
     */
    public String explain(
            String ingredientName,
            List<ShoppingItemDto> items,
            String recipeTitle,
            List<String> matchedIngredients,
            List<String> missingIngredients) {
        try {
            List<ShoppingItemRequest> shoppingItems = items.stream()
                    .limit(2)
                    .map(item -> new ShoppingItemRequest(
                            item.getMallName(),
                            item.getProductName(),
                            item.getPrice(),
                            item.getShippingType() != null ? item.getShippingType().name() : "STANDARD",
                            item.getOriginalPrice(),
                            item.getDiscountRate() != null ? item.getDiscountRate().intValue() : null
                    ))
                    .toList();

            int lowestPrice = items.stream()
                    .mapToInt(ShoppingItemDto::getPrice)
                    .min()
                    .orElse(0);

            ExplainRequest request = new ExplainRequest(
                    ingredientName,
                    recipeTitle,
                    null,
                    matchedIngredients != null ? matchedIngredients : List.of(),
                    missingIngredients != null ? missingIngredients : List.of(),
                    shoppingItems,
                    lowestPrice
            );

            ExplainResponse response = restTemplate.postForObject(explainUrl, request, ExplainResponse.class);
            return response != null ? response.explanation() : null;

        } catch (Exception e) {
            log.warn("[ShoppingExplainClient] 추천 이유 생성 실패 (ingredient={}): {}", ingredientName, e.getMessage());
            return null;
        }
    }

    /** 레시피 컨텍스트 없이 가격만으로 설명 요청 */
    public String explain(String ingredientName, List<ShoppingItemDto> items) {
        return explain(ingredientName, items, null, List.of(), List.of());
    }

    // ── 내부 DTO ────────────────────────────────────────────────────────────

    record ShoppingItemRequest(
            String mallName,
            String productName,
            Integer price,
            String shippingType,
            Integer originalPrice,
            Integer discountRate
    ) {}

    record ExplainRequest(
            String ingredientName,
            String recipeTitle,
            Integer recipeId,
            List<String> matchedIngredients,
            List<String> missingIngredients,
            List<ShoppingItemRequest> shoppingItems,
            Integer lowestPrice
    ) {}

    record ExplainResponse(String explanation) {}
}

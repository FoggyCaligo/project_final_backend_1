package com.today.fridge.shopping.controller;

import com.today.fridge.global.exception.GlobalExceptionHandler;
import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.recipe.service.RecipeService;
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.service.ShoppingService3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShoppingController 단위 테스트 (WebMvcTest)
 * - ShoppingService3, RecipeService는 Mock 처리
 * - Spring Security 필터는 제외하여 인증 로직과 분리
 * - UNAUTHORIZED 검증은 컨트롤러 내부 requireUserId() 로직 기준
 */
@WebMvcTest(
        value = ShoppingController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class ShoppingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShoppingService3 shoppingService3;

    @MockBean
    private RecipeService recipeService;

    private IngredientPriceResponse sampleResponse() {
        return IngredientPriceResponse.builder()
                .ingredientId(null)
                .ingredientName("계란")
                .lowestPrice(2800)
                .items(List.of())
                .cachedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // ============================================================
    // GET /api/v1/shopping/search
    // ============================================================

    @Test
    @DisplayName("[단위] GET /search?keyword=계란: explanation 필드가 응답 JSON에 포함된다")
    void search_withExplanation_includesExplanationInJson() throws Exception {
        IngredientPriceResponse withExplanation = IngredientPriceResponse.builder()
                .ingredientId(null).ingredientName("계란").lowestPrice(2800)
                .items(List.of()).cachedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .explanation("네이버쇼핑에서 무료배송으로 저렴하게 구매할 수 있습니다.")
                .build();
        given(shoppingService3.searchByKeyword("계란")).willReturn(withExplanation);

        mockMvc.perform(get("/api/v1/shopping/search").param("keyword", "계란"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation").value("네이버쇼핑에서 무료배송으로 저렴하게 구매할 수 있습니다."));
    }

    @Test
    @DisplayName("[단위] GET /search?keyword=계란: explanation이 null이면 JSON에서 해당 필드가 생략된다")
    void search_withoutExplanation_omitsExplanationFromJson() throws Exception {
        given(shoppingService3.searchByKeyword("계란")).willReturn(sampleResponse()); // explanation=null

        mockMvc.perform(get("/api/v1/shopping/search").param("keyword", "계란"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation").doesNotExist());
    }

    @Test
    @DisplayName("[단위] GET /search?keyword=계란: 키워드 검색 성공 시 200과 결과를 반환한다")
    void search_validKeyword_returns200() throws Exception {
        given(shoppingService3.searchByKeyword("계란")).willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/shopping/search").param("keyword", "계란"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ingredientName").value("계란"))
                .andExpect(jsonPath("$.data.lowestPrice").value(2800));
    }

    @Test
    @DisplayName("[단위] GET /search?keyword= (빈값): 빈 keyword면 400을 반환한다")
    void search_emptyKeyword_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/search").param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("[단위] GET /search: keyword 파라미터 자체가 없으면 400을 반환한다")
    void search_missingKeyword_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/search"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/v1/shopping/ingredients/{id}/prices
    // ============================================================

    @Test
    @DisplayName("[단위] GET /ingredients/1/prices: X-User-Id 없으면 UNAUTHORIZED를 반환한다")
    void getIngredientPrices_noUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/ingredients/1/prices"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("[단위] GET /ingredients/1/prices: X-User-Id 있으면 200과 결과를 반환한다")
    void getIngredientPrices_withUserId_returns200() throws Exception {
        IngredientPriceResponse resp = IngredientPriceResponse.builder()
                .ingredientId(1L).ingredientName("계란").lowestPrice(3000)
                .items(List.of()).cachedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .build();
        given(shoppingService3.getIngredientPrices(1L)).willReturn(resp);

        mockMvc.perform(get("/api/v1/shopping/ingredients/1/prices")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ingredientId").value(1))
                .andExpect(jsonPath("$.data.lowestPrice").value(3000));
    }

    // ============================================================
    // GET /api/v1/shopping/fridge/prices
    // ============================================================

    @Test
    @DisplayName("[단위] GET /fridge/prices: X-User-Id 없으면 UNAUTHORIZED를 반환한다")
    void getFridgePrices_noUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/fridge/prices"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("[단위] GET /fridge/prices: X-User-Id 있으면 200과 빈 리스트를 반환한다")
    void getFridgePrices_withUserId_returns200() throws Exception {
        given(shoppingService3.getFridgePrices(anyLong())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/shopping/fridge/prices")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ============================================================
    // GET /api/v1/shopping/recipes/{recipeId}/missing-ingredients-prices
    // ============================================================

    @Test
    @DisplayName("[단위] GET /recipes/1/missing-ingredients-prices: X-User-Id 없으면 UNAUTHORIZED를 반환한다")
    void getMissingIngredientsPrices_noUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/recipes/1/missing-ingredients-prices"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("[단위] GET /recipes/1/missing-ingredients-prices: MISSING 재료 있으면 결과 1건 반환")
    void getMissingIngredientsPrices_withMissingIngredient_returns1Result() throws Exception {
        RecipeIngredientDTO missingIng = RecipeIngredientDTO.builder()
                .normalizedNameSnapshot("수박").sufficiency("MISSING").build();
        RecipeIngredientDTO okIng = RecipeIngredientDTO.builder()
                .normalizedNameSnapshot("소금").sufficiency("OK").build();
        RecipeResponse recipeResponse = RecipeResponse.builder()
                .recipeIngredients(List.of(missingIng, okIng)).build();

        given(recipeService.getRecipe(1L, 1L)).willReturn(recipeResponse);

        IngredientPriceResponse watermelonPrice = IngredientPriceResponse.builder()
                .ingredientName("수박").lowestPrice(15000).items(List.of())
                .cachedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
        given(shoppingService3.searchByKeyword("수박")).willReturn(watermelonPrice);

        mockMvc.perform(get("/api/v1/shopping/recipes/1/missing-ingredients-prices")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ingredientName").value("수박"));
    }

    @Test
    @DisplayName("[단위] GET /recipes/1/missing-ingredients-prices: 모든 재료 OK면 빈 리스트 반환")
    void getMissingIngredientsPrices_allOk_returnsEmptyList() throws Exception {
        RecipeIngredientDTO egg = RecipeIngredientDTO.builder()
                .normalizedNameSnapshot("계란").sufficiency("OK").build();
        RecipeIngredientDTO green = RecipeIngredientDTO.builder()
                .normalizedNameSnapshot("파").sufficiency("OK").build();
        RecipeResponse recipeResponse = RecipeResponse.builder()
                .recipeIngredients(List.of(egg, green)).build();

        given(recipeService.getRecipe(1L, 1L)).willReturn(recipeResponse);

        mockMvc.perform(get("/api/v1/shopping/recipes/1/missing-ingredients-prices")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}

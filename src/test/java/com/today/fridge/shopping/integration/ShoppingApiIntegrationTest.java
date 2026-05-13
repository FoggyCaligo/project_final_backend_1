package com.today.fridge.shopping.integration;

import com.today.fridge.global.external.EmailService;
import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.external.ai.ShoppingExplainClient;
import com.today.fridge.shopping.external.elevenst.ElevenStShoppingClient;
import com.today.fridge.shopping.external.naver.NaverShoppingClient;
import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shopping API 통합 테스트
 * - H2 인메모리 DB, 전체 Spring 컨텍스트 로딩
 * - NaverShoppingClient, ElevenStShoppingClient, RedisTemplate은 Mock 처리
 *   (외부 API 키 없이 동작 검증)
 * - ShoppingService3 → ShoppingController 전 계층 통합 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShoppingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private NaverShoppingClient naverClient;

    @MockBean
    private ElevenStShoppingClient elevenStClient;

    @MockBean
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockBean
    private ShoppingExplainClient shoppingExplainClient;

    @MockBean
    private EmailService emailService;

    @MockBean
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;

    private long ingredientId;

    @BeforeEach
    void setUp() {
        // Redis Mock 세팅 — 캐시 MISS 상태로 설정
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);

        // H2 DB에 ingredient_master 데이터 삽입
        jdbcTemplate.update("DELETE FROM shopping_item_mcp WHERE ingredient_master_id IN (SELECT ingredient_id FROM ingredient_master WHERE canonical_name = ?)", "계란");
        jdbcTemplate.update("DELETE FROM ingredient_master WHERE canonical_name = ?", "계란");
        jdbcTemplate.update(
                "INSERT INTO ingredient_master (canonical_name, normalized_name) VALUES (?, ?)",
                "계란", "계란");
        ingredientId = jdbcTemplate.queryForObject(
                "SELECT ingredient_id FROM ingredient_master WHERE canonical_name = ?",
                Long.class, "계란");
    }

    // ============================================================
    // GET /api/v1/shopping/search
    // ============================================================

    @Test
    @DisplayName("[통합] GET /search?keyword=계란: 네이버+11번가 Mock으로 최저가 응답을 반환한다")
    void search_keyword_returns_lowest_price() throws Exception {
        ShoppingItemDto naverItem = ShoppingItemDto.builder()
                .mallName("네이버쇼핑").mallProductId("n1").productName("계란 30구")
                .price(3000).shippingType(ShippingType.FREE).stockStatus(StockStatus.IN_STOCK).build();
        ShoppingItemDto elevenItem = ShoppingItemDto.builder()
                .mallName("11번가").mallProductId("e1").productName("계란 30개")
                .price(2700).shippingType(ShippingType.STANDARD).stockStatus(StockStatus.IN_STOCK).build();

        given(naverClient.search("계란")).willReturn(List.of(naverItem));
        given(elevenStClient.search("계란")).willReturn(List.of(elevenItem));

        mockMvc.perform(get("/api/v1/shopping/search")
                        .param("keyword", "계란")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ingredientName").value("계란"))
                .andExpect(jsonPath("$.data.lowestPrice").value(2700))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    @DisplayName("[통합] GET /search: keyword 파라미터 없으면 400을 반환한다")
    void search_missing_keyword_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/search")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[통합] GET /search?keyword=계란: 모든 외부 API가 빈 결과이면 빈 items를 반환한다")
    void search_all_api_empty_returns_empty_items() throws Exception {
        given(naverClient.search(anyString())).willReturn(Collections.emptyList());
        given(elevenStClient.search(anyString())).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/shopping/search")
                        .param("keyword", "계란")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.lowestPrice").value(0));
    }

    // ============================================================
    // GET /api/v1/shopping/ingredients/{id}/prices
    // ============================================================

    @Test
    @DisplayName("[통합] GET /ingredients/{id}/prices: X-User-Id 없으면 UNAUTHORIZED를 반환한다")
    void getIngredientPrices_noAuth_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/ingredients/" + ingredientId + "/prices")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("[통합] GET /ingredients/{id}/prices: 유효한 ingredientId + X-User-Id로 200을 반환한다")
    void getIngredientPrices_valid_returns_200() throws Exception {
        ShoppingItemDto naverItem = ShoppingItemDto.builder()
                .mallName("네이버쇼핑").mallProductId("n1").productName("계란 30구")
                .price(3200).shippingType(ShippingType.FREE).stockStatus(StockStatus.IN_STOCK).build();
        given(naverClient.search("계란")).willReturn(List.of(naverItem));
        given(elevenStClient.search("계란")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/shopping/ingredients/" + ingredientId + "/prices")
                        .header("X-User-Id", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ingredientName").value("계란"))
                .andExpect(jsonPath("$.data.lowestPrice").value(3200));
    }

    @Test
    @DisplayName("[통합] GET /ingredients/99999/prices: 존재하지 않는 ingredientId는 에러를 반환한다")
    void getIngredientPrices_unknownId_returns_error() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/ingredients/99999/prices")
                        .header("X-User-Id", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }

    // ============================================================
    // GET /api/v1/shopping/fridge/prices
    // ============================================================

    @Test
    @DisplayName("[통합] GET /fridge/prices: X-User-Id 없으면 UNAUTHORIZED를 반환한다")
    void getFridgePrices_noAuth_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/fridge/prices")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("[통합] GET /fridge/prices: 냉장고에 재료 없는 사용자는 빈 리스트를 반환한다")
    void getFridgePrices_emptyFridge_returns_empty_list() throws Exception {
        // 사용자 냉장고에 재료가 없으므로 빈 응답 반환
        mockMvc.perform(get("/api/v1/shopping/fridge/prices")
                        .header("X-User-Id", 999L) // 존재하지 않는 userId (냉장고 비어있음)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ============================================================
    // GET /api/v1/shopping/recipes/{recipeId}/missing-ingredients-prices
    // ============================================================

    @Test
    @DisplayName("[통합] GET /recipes/{id}/missing-ingredients-prices: X-User-Id 없으면 UNAUTHORIZED를 반환한다")
    void getMissingIngredientsPrices_noUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/shopping/recipes/1/missing-ingredients-prices")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ============================================================
    // explanation 포함 여부 검증
    // ============================================================

    @Test
    @DisplayName("[통합] GET /search: ShoppingExplainClient가 설명을 반환하면 응답에 explanation이 포함된다")
    void search_withExplanation_includesExplanationInResponse() throws Exception {
        ShoppingItemDto naverItem = ShoppingItemDto.builder()
                .mallName("네이버쇼핑").mallProductId("n1").productName("계란 30구")
                .price(3000).shippingType(ShippingType.FREE).stockStatus(StockStatus.IN_STOCK).build();

        given(naverClient.search("계란")).willReturn(List.of(naverItem));
        given(elevenStClient.search("계란")).willReturn(Collections.emptyList());
        given(shoppingExplainClient.explain(anyString(), any()))
                .willReturn("네이버쇼핑에서 무료배송으로 저렴하게 구매 가능합니다.");

        mockMvc.perform(get("/api/v1/shopping/search")
                        .param("keyword", "계란")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation")
                        .value("네이버쇼핑에서 무료배송으로 저렴하게 구매 가능합니다."));
    }

    @Test
    @DisplayName("[통합] GET /search: ShoppingExplainClient가 null 반환해도 나머지 필드는 정상 응답된다")
    void search_explainReturnsNull_otherFieldsStillPresent() throws Exception {
        ShoppingItemDto naverItem = ShoppingItemDto.builder()
                .mallName("네이버쇼핑").mallProductId("n1").productName("대파 1단")
                .price(980).shippingType(ShippingType.FREE).stockStatus(StockStatus.IN_STOCK).build();

        given(naverClient.search("대파")).willReturn(List.of(naverItem));
        given(elevenStClient.search("대파")).willReturn(Collections.emptyList());
        given(shoppingExplainClient.explain(anyString(), any())).willReturn(null);

        mockMvc.perform(get("/api/v1/shopping/search")
                        .param("keyword", "대파")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowestPrice").value(980))
                .andExpect(jsonPath("$.data.explanation").doesNotExist());
    }

    // JPA 모킹 없이 deleteExpired 부작용 제거를 위한 헬퍼
    private Object shoppingItemRepositoryDeleteExpired() {
        return null;
    }
}

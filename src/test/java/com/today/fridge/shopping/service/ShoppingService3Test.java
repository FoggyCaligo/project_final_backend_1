package com.today.fridge.shopping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.entity.ShoppingItem;
import com.today.fridge.shopping.external.elevenst.ElevenStShoppingClient;
import com.today.fridge.shopping.external.naver.NaverShoppingClient;
import com.today.fridge.shopping.repository.ShoppingItemRepository;
import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * ShoppingService3 단위 테스트
 * - 외부 API(Naver, 11번가), Redis, DB 레포지토리 모두 Mock 처리
 * - searchByKeyword / getIngredientPrices의 캐시 전략(Redis HIT/MISS, DB HIT/MISS) 검증
 */
@ExtendWith(MockitoExtension.class)
class ShoppingService3Test {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @Mock
    private IngredientMasterRepository ingredientMasterRepository;

    @Mock
    private NaverShoppingClient naverClient;

    @Mock
    private ElevenStShoppingClient elevenStClient;

    @Mock
    private EntityManager em;

    @InjectMocks
    private ShoppingService3 shoppingService3;

    private IngredientMaster masterEgg;

    @BeforeEach
    void setUp() {
        // EntityManager는 @PersistenceContext로 주입되어 @InjectMocks가 처리하지 못하므로 직접 주입
        ReflectionTestUtils.setField(shoppingService3, "em", em);

        // RedisTemplate의 opsForValue() 스텁
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        masterEgg = IngredientMaster.builder()
                .ingredientMasterId(1L)
                .canonicalName("계란")
                .normalizedName("계란")
                .build();
    }

    // ============================================================
    // searchByKeyword — Redis 캐시 HIT
    // ============================================================

    @Test
    @DisplayName("[단위] searchByKeyword: Redis 캐시 HIT 시 외부 API를 호출하지 않고 캐시를 반환한다")
    void searchByKeyword_redisCacheHit_returnsCache() {
        // given
        IngredientPriceResponse cached = IngredientPriceResponse.builder()
                .ingredientId(null)
                .ingredientName("계란")
                .lowestPrice(2500)
                .items(List.of())
                .cachedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        given(valueOps.get("shopping:keyword:계란")).willReturn(cached);

        // when
        IngredientPriceResponse result = shoppingService3.searchByKeyword("계란");

        // then
        assertThat(result.getLowestPrice()).isEqualTo(2500);
        then(naverClient).should(never()).search(anyString());
        then(elevenStClient).should(never()).search(anyString());
    }

    // ============================================================
    // searchByKeyword — Redis MISS → 외부 API 호출
    // ============================================================

    @Test
    @DisplayName("[단위] searchByKeyword: Redis MISS 시 네이버+11번가를 호출하고 최저가를 반환한다")
    void searchByKeyword_redisMiss_callsExternalApis() {
        // given — Redis MISS
        given(valueOps.get(startsWith("shopping:keyword:"))).willReturn(null);

        ShoppingItemDto naverItem = ShoppingItemDto.builder()
                .mallName("네이버쇼핑").mallProductId("naver-1").productName("계란 30구")
                .price(3000).shippingType(ShippingType.FREE).stockStatus(StockStatus.IN_STOCK).build();
        ShoppingItemDto elevenItem = ShoppingItemDto.builder()
                .mallName("11번가").mallProductId("11st-1").productName("계란 30개입")
                .price(2800).shippingType(ShippingType.STANDARD).stockStatus(StockStatus.IN_STOCK).build();

        given(naverClient.search("계란")).willReturn(List.of(naverItem));
        given(elevenStClient.search("계란")).willReturn(List.of(elevenItem));

        // when
        IngredientPriceResponse result = shoppingService3.searchByKeyword("계란");

        // then — 11번가가 더 저렴하므로 lowestPrice는 2800
        assertThat(result.getIngredientName()).isEqualTo("계란");
        assertThat(result.getLowestPrice()).isEqualTo(2800);
        assertThat(result.getItems()).hasSize(2);
    }

    // ============================================================
    // searchByKeyword — 모든 API 실패 → 빈 응답
    // ============================================================

    @Test
    @DisplayName("[단위] searchByKeyword: 모든 외부 API가 빈 결과를 반환하면 빈 응답을 반환한다 (예외 없음)")
    void searchByKeyword_allApisFail_returnsEmptyResponse() {
        // given
        given(valueOps.get(anyString())).willReturn(null);
        given(naverClient.search(anyString())).willReturn(Collections.emptyList());
        given(elevenStClient.search(anyString())).willReturn(Collections.emptyList());

        // when
        IngredientPriceResponse result = shoppingService3.searchByKeyword("존재하지않는재료xyz");

        // then — 예외 없이 빈 응답
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getLowestPrice()).isEqualTo(0);
    }

    // ============================================================
    // getIngredientPrices — Redis HIT
    // ============================================================

    @Test
    @DisplayName("[단위] getIngredientPrices: Redis 캐시 HIT 시 외부 API 및 DB를 조회하지 않는다")
    void getIngredientPrices_redisCacheHit_returnsCache() {
        // given
        IngredientPriceResponse cached = IngredientPriceResponse.builder()
                .ingredientId(1L).ingredientName("계란").lowestPrice(2500)
                .items(List.of()).cachedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .build();
        given(valueOps.get("shopping:price:1")).willReturn(cached);

        // when
        IngredientPriceResponse result = shoppingService3.getIngredientPrices(1L);

        // then
        assertThat(result.getLowestPrice()).isEqualTo(2500);
        then(ingredientMasterRepository).should(never()).findById(anyLong());
        then(naverClient).should(never()).search(anyString());
    }

    // ============================================================
    // getIngredientPrices — DB 캐시 HIT
    // ============================================================

    @Test
    @DisplayName("[단위] getIngredientPrices: Redis MISS + DB 캐시 HIT 시 DB 결과를 반환하고 Redis에 재저장한다")
    void getIngredientPrices_dbCacheHit_returnsDbAndCachesToRedis() {
        // given — Redis MISS
        given(valueOps.get("shopping:price:1")).willReturn(null);
        given(ingredientMasterRepository.findById(1L)).willReturn(Optional.of(masterEgg));

        ShoppingItem dbItem = ShoppingItem.builder()
                .shoppingItemId(10L).ingredientMaster(masterEgg)
                .mallName("네이버쇼핑").productName("계란 30구").price(2500)
                .fetchedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .stockStatus(StockStatus.IN_STOCK).shippingType(ShippingType.FREE)
                .shippingFee(0).reviewCount(0).updatedAt(Instant.now()).build();
        given(shoppingItemRepository
                .findByIngredientMaster_IngredientMasterIdAndExpiresAtAfterOrderByPriceAsc(eq(1L), any(Instant.class)))
                .willReturn(List.of(dbItem));

        // when
        IngredientPriceResponse result = shoppingService3.getIngredientPrices(1L);

        // then
        assertThat(result.getIngredientName()).isEqualTo("계란");
        assertThat(result.getLowestPrice()).isEqualTo(2500);
        // Redis에 재저장 검증
        then(valueOps).should().set(eq("shopping:price:1"), any(), anyLong(), any());
        then(naverClient).should(never()).search(anyString());
    }

    // ============================================================
    // getIngredientPrices — 완전 캐시 MISS → 외부 API
    // ============================================================

    @Test
    @DisplayName("[단위] getIngredientPrices: Redis + DB 모두 MISS 시 외부 API 호출 후 DB에 저장한다")
    void getIngredientPrices_allMiss_callsApisAndSavesToDb() {
        // given — Redis MISS
        given(valueOps.get("shopping:price:1")).willReturn(null);
        given(ingredientMasterRepository.findById(1L)).willReturn(Optional.of(masterEgg));
        // DB 캐시 MISS
        given(shoppingItemRepository
                .findByIngredientMaster_IngredientMasterIdAndExpiresAtAfterOrderByPriceAsc(eq(1L), any(Instant.class)))
                .willReturn(Collections.emptyList());

        ShoppingItemDto naverItem = ShoppingItemDto.builder()
                .mallName("네이버쇼핑").mallProductId("n1").productName("계란 30구")
                .price(3000).shippingType(ShippingType.FREE).stockStatus(StockStatus.IN_STOCK).build();
        given(naverClient.search("계란")).willReturn(List.of(naverItem));
        given(elevenStClient.search("계란")).willReturn(Collections.emptyList());
        given(shoppingItemRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // when
        IngredientPriceResponse result = shoppingService3.getIngredientPrices(1L);

        // then
        assertThat(result.getLowestPrice()).isEqualTo(3000);
        then(shoppingItemRepository).should().saveAll(anyList());
    }
}

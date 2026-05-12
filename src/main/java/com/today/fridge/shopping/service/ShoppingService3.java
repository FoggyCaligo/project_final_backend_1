package com.today.fridge.shopping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.entity.ShoppingItem;
import com.today.fridge.shopping.exception.ShoppingErrorCode;
// import com.today.fridge.shopping.external.coupang.CoupangShoppingClient2;
import com.today.fridge.shopping.external.elevenst.ElevenStShoppingClient;
import com.today.fridge.shopping.external.naver.NaverShoppingClient;
import com.today.fridge.shopping.repository.ShoppingItemRepository;
import com.today.fridge.shopping.type.StockStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Redis 캐시 기반 식재료 최저가 검색 서비스
 *
 * 캐시 전략:
 * 1. Redis 캐시 확인 (TTL 1시간)
 * 2. Redis Miss → 네이버+11번가 병렬 API 호출
 * 3. Redis 저장 + DB 저장 (DB는 Redis 장애 시 fallback)
 *
 * Redis 장애 시 DB 캐시(shopping_item_mcp)로 자동 fallback합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingService3 {

    private static final long CACHE_HOURS = 1;
    private static final String PRICE_PREFIX = "shopping:price:";
    private static final String FRIDGE_PREFIX = "shopping:fridge:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ShoppingItemRepository shoppingItemRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final NaverShoppingClient naverClient;

    private final ElevenStShoppingClient elevenStClient;
    // private final CoupangShoppingClient2 coupangClient;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public IngredientPriceResponse getIngredientPrices(Long ingredientMasterId) {
        // 1. Redis 캐시 확인
        //
        /**
         * Redis 는 메모리에 쓰는 포스트잇 메모장 같은 역할
         * opsForValue() : Redis 의 여러 데이터 구조 중 가장 기본인 String(Key-value) 형태를 다룬다, 키에 해당하는
         * 값을 가져오거나 저장할때 사용
         * get(key) : 키에 따른 값을 읽는다
         * set(key, value, timeout, unit) 키에 따른 값을 저장하고 몇분 뒤에 파기해 라고 타이머를 설정
         * opsForList() : 리스트(순서가 있는 목록)을 다룰때
         * opsForHash(): 객체처럼 여러 필드가 있는 데이터를 다룰때
         * delete(key) : 삭제
         */
        try {
            // cashed 는 PRICE_PREFIX (shopping:price)에 식재료id를 붙인 키를 Redis 에서 조회 한 결과를 받아서
            // 변수에 저장
            Object cached = redisTemplate.opsForValue().get(PRICE_PREFIX + ingredientMasterId);
            if (cached != null) {
                log.debug("[ShoppingService3] Redis 캐시 HIT ingredientId={}", ingredientMasterId);
                return objectMapper.convertValue(cached, IngredientPriceResponse.class);
            }
        } catch (Exception e) {
            log.warn("[ShoppingService3] Redis 캐시 조회 실패, DB fallback: {}", e.getMessage());
        }

        // 2. DB 캐시 확인 (Redis 장애 시 fallback)
        IngredientMaster master = ingredientMasterRepository.findById(ingredientMasterId)
                .orElseThrow(ShoppingErrorCode.INGREDIENT_MASTER_NOT_FOUND::toException);
        // DB 를 조회하지 못하면 '식재료 마스터 정보를 찾을 수 없습니다.' 문구 출력
        Instant now = Instant.now();
        // dbCached는 shoppingItem 테이블에서 식재료 id 기준으로 데이터를 조회해 가격순서대로 db를 조회한 결과를 받아서 변수에
        // 저장
        List<ShoppingItem> dbCached = shoppingItemRepository
                .findByIngredientMaster_IngredientMasterIdAndExpiresAtAfterOrderByPriceAsc(
                        master.getIngredientMasterId(), now);

        if (!dbCached.isEmpty()) {
            log.debug("[ShoppingService3] DB 캐시 HIT ingredientId={}", ingredientMasterId);
            IngredientPriceResponse response = buildResponse(master, dbCached);
            // DB 캐시 결과를 Redis에도 저장
            cacheToRedis(PRICE_PREFIX + ingredientMasterId, response);
            return response;
        }

        // 3. 캐시 Miss (레디쉬, DB 캐시 둘다 없을때) → 외부 API 호출
        IngredientPriceResponse response = fetchFromExternalApis(master, now);

        // 4. Redis 캐시 저장 (외부 Api를 가져와 1시간동안 저장)
        cacheToRedis(PRICE_PREFIX + ingredientMasterId, response);

        return response;
    }

    @Transactional
    public List<IngredientPriceResponse> getFridgePrices(Long userId) {
        // 1. Redis 캐시 확인
        try {
            // cashed 는 FRIDGE_PREFIX (shopping:fridge:유저아이디) 를 레디쉬에서 가져와 저장
            Object cached = redisTemplate.opsForValue().get(FRIDGE_PREFIX + userId);
            if (cached != null) {
                log.debug("[ShoppingService3] Redis 냉장고 캐시 HIT userId={}", userId);
                // List<LinkedHashMap> → List<IngredientPriceResponse> 변환
                @SuppressWarnings("unchecked")

                // stream 방식은 for문을 길게 쓰는 것보다 코드가 간결하고 무슨 일을 하는지 보기 좋은 가독성을 가진다
                // stream() : 리스트 (데이터 덩어리)를 컨베이어 밸트에 하나씩 올린다
                // map() : 컨베이어 밸트 위를 지나가는 데이터 하나하나를 다른 형태 (가격 정보)로 재가공
                // filter() : 조건에 안맞는 데이터를 벨트 밖으로 밀어낸다
                // toList() : 가공이 끝난 데이터를 다시 list로 담는다
                // ---------------------------------------------------
                // List<LinkedHashMap> → List<IngredientPriceResponse> 변환

                // rawList 는 cached 를 List<Object> 로 변환
                List<Object> rawList = (List<Object>) cached;

                return rawList.stream()
                        .map(item -> objectMapper.convertValue(item, IngredientPriceResponse.class))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("[ShoppingService3] Redis 냉장고 캐시 조회 실패: {}", e.getMessage());
        }

        // 2. 냉장고 식재료 목록 조회
        List<IngredientMaster> masters = em.createQuery(
                "SELECT DISTINCT ui.ingredientMaster FROM UserIngredient ui " +
                        "WHERE ui.user.userId = :userId AND ui.ingredientMaster IS NOT NULL",
                IngredientMaster.class)
                .setParameter("userId", userId)
                .getResultList();

        if (masters.isEmpty()) {
            return List.of();
        }

        // results는
        List<IngredientPriceResponse> results = masters.stream()
                .map(master -> getIngredientPrices(master.getIngredientMasterId()))
                .toList();

        // 3. Redis 캐시 저장 (30분)
        try {
            redisTemplate.opsForValue().set(
                    FRIDGE_PREFIX + userId, results, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[ShoppingService3] Redis 냉장고 캐시 저장 실패: {}", e.getMessage());
        }

        return results;
    }

    // ── 내부 메서드 ──

    private IngredientPriceResponse fetchFromExternalApis(IngredientMaster master, Instant now) {
        String keyword = master.getCanonicalName();

        // 네이버와 11번가 동시에 비동기 요청
        CompletableFuture<List<ShoppingItemDto>> naverFuture = CompletableFuture
                .supplyAsync(() -> naverClient.search(keyword));
        CompletableFuture<List<ShoppingItemDto>> elevenFuture = CompletableFuture
                .supplyAsync(() -> elevenStClient.search(keyword));

        List<ShoppingItemDto> naverItems = naverFuture.join();
        List<ShoppingItemDto> elevenStItems = elevenFuture.join()
                .stream()
                .min(Comparator.comparingInt(ShoppingItemDto::getPrice))
                .map(List::of)
                .orElse(List.of());

        List<ShoppingItemDto> allItems = Stream.concat(naverItems.stream(), elevenStItems.stream())
                .sorted(Comparator.comparingInt(ShoppingItemDto::getPrice))
                .toList();
        if (allItems.isEmpty()) {
            throw ShoppingErrorCode.ALL_SHOPPING_API_FAILED.toException();
        }

        // DB에도 저장 (Redis 장애 시 fallback용)
        Instant expiresAt = now.plus(CACHE_HOURS, ChronoUnit.HOURS);
        List<ShoppingItem> toSave = allItems.stream()
                .map(dto -> toEntity(dto, master, now, expiresAt))
                .toList();

        shoppingItemRepository.saveAll(toSave);
        cleanupExpiredAsync(now);

        return buildResponse(master, toSave);
    }

    private void cacheToRedis(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[ShoppingService3] Redis 캐시 저장 실패: {}", e.getMessage());
        }
    }

    // ShoppingItem 은 식재료마스터DB에 mallName(네이버나 11번가 등), mallProductId, 구메아이템이름, 브랜드,
    // 할인가격, 원래 가격, 할인비율, 이미지url, 쇼핑타입, 재고현황, 배송료, 리뷰평점, 받아온 시간, 만료시간, 업데이트 시간의 정보를
    // 담는다.
    private ShoppingItem toEntity(ShoppingItemDto dto, IngredientMaster master,
            Instant fetchedAt, Instant expiresAt) {
        return ShoppingItem.builder()
                .ingredientMaster(master)
                .mallName(dto.getMallName())
                .mallProductId(dto.getMallProductId())
                .productName(dto.getProductName())
                .brand(dto.getBrand())
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .discountRate(dto.getDiscountRate())
                .purchaseUrl(dto.getPurchaseUrl())
                .imageUrl(dto.getImageUrl())
                .shippingType(dto.getShippingType())
                .stockStatus(dto.getStockStatus() != null ? dto.getStockStatus() : StockStatus.IN_STOCK)
                .shippingFee(0)
                .reviewCount(0)
                .fetchedAt(fetchedAt)
                .expiresAt(expiresAt)
                .updatedAt(fetchedAt)
                .build();
    }

    private IngredientPriceResponse buildResponse(IngredientMaster master, List<ShoppingItem> items) {
        List<ShoppingItemDto> dtos = items.stream()
                .map(item -> ShoppingItemDto.builder()
                        .mallName(item.getMallName())
                        .mallProductId(item.getMallProductId())
                        .productName(item.getProductName())
                        .brand(item.getBrand())
                        .price(item.getPrice())
                        .originalPrice(item.getOriginalPrice())
                        .discountRate(item.getDiscountRate())
                        .purchaseUrl(item.getPurchaseUrl())
                        .imageUrl(item.getImageUrl())
                        .shippingType(item.getShippingType())
                        .stockStatus(item.getStockStatus())
                        .build())
                .toList();

        int lowestPrice = items.stream()
                .mapToInt(ShoppingItem::getPrice)
                .min()
                .orElse(0);

        Instant cachedAt = items.stream()
                .map(ShoppingItem::getFetchedAt)
                .min(Instant::compareTo)
                .orElse(Instant.now());

        Instant expiresAt = items.stream()
                .map(ShoppingItem::getExpiresAt)
                .max(Instant::compareTo)
                .orElse(Instant.now());

        return IngredientPriceResponse.builder()
                .ingredientId(master.getIngredientMasterId())
                .ingredientName(master.getCanonicalName())
                .lowestPrice(lowestPrice)
                .items(dtos)
                .cachedAt(cachedAt)
                .expiresAt(expiresAt)
                .build();
    }

    // 백그라운드에서 만료된 DB 캐시 정리
    private void cleanupExpiredAsync(Instant now) {
        CompletableFuture.runAsync(() -> {
            try {
                shoppingItemRepository.deleteExpired(now);
            } catch (Exception e) {
                log.warn("[ShoppingService3] 만료 캐시 삭제 실패: {}", e.getMessage());
            }
        });
    }

    // ── 키워드 기반 실시간 검색 (ingredient_master 없이 직접 API 호출) ──

    private static final String KEYWORD_PREFIX = "shopping:keyword:";

    /**
     * 키워드로 직접 네이버+11번가 API를 호출하여 실시간 최저가를 검색합니다.
     * ingredient_master 테이블에 없는 식재료도 검색 가능합니다.
     *
     * @param keyword 검색어 (예: "계란", "대파", "양파")
     * @return 검색 결과 IngredientPriceResponse
     */
    public IngredientPriceResponse searchByKeyword(String keyword) {
        String normalizedKeyword = keyword.trim().toLowerCase();

        // 1. Redis 캐시 확인
        try {
            Object cached = redisTemplate.opsForValue().get(KEYWORD_PREFIX + normalizedKeyword);
            if (cached != null) {
                log.debug("[ShoppingService3] 키워드 캐시 HIT keyword={}", normalizedKeyword);
                return objectMapper.convertValue(cached, IngredientPriceResponse.class);
            }
        } catch (Exception e) {
            log.warn("[ShoppingService3] 키워드 캐시 조회 실패: {}", e.getMessage());
        }

        // 2. 네이버 + 11번가 병렬 API 호출
        CompletableFuture<List<ShoppingItemDto>> naverFuture = CompletableFuture
                .supplyAsync(() -> naverClient.search(normalizedKeyword));
        CompletableFuture<List<ShoppingItemDto>> elevenStFuture = CompletableFuture
                .supplyAsync(() -> elevenStClient.search(normalizedKeyword));

        // 네이버는 NaverClient 내부에서 이미 min() 1개 반환, 11번가는 여러 개 중 최저가 1개만 사용
        // List<ShoppingItemDto> naverItems = naverFuture.join();

        ShoppingItemDto bestNaver = naverFuture.join().stream()
                .min(Comparator.comparingInt(ShoppingItemDto::getPrice))
                .orElse(null);

        // List<ShoppingItemDto> elevenItems = elevenStFuture.join();
        ShoppingItemDto bestEleven = elevenStFuture.join().stream()
                .min(Comparator.comparingInt(ShoppingItemDto::getPrice))
                .orElse(null);

        // List<ShoppingItemDto> elevenStItems = elevenStFuture.join().stream()
        // .min(Comparator.comparingInt(ShoppingItemDto::getPrice))
        // .map(List::of)
        // .orElse(List.of());

        // 두 개를 합침
        List<ShoppingItemDto> allItems = Stream.of(bestNaver, bestEleven)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(ShoppingItemDto::getPrice)) // 전체 중 더 싼 게 앞으로 오게 정렬
                .toList();

        if (allItems.isEmpty()) {
            // 검색 결과 없을 때 빈 응답 반환 (예외 대신)
            return IngredientPriceResponse.builder()
                    .ingredientId(null)
                    .ingredientName(keyword)
                    .lowestPrice(0)
                    .items(List.of())
                    .cachedAt(Instant.now())
                    .expiresAt(Instant.now().plus(CACHE_HOURS, ChronoUnit.HOURS))
                    .build();
        }

        // int lowestPrice =
        // allItems.stream().mapToInt(ShoppingItemDto::getPrice).min().orElse(0);
        int lowestPrice = allItems.get(0).getPrice(); // 정렬되었으므로 첫 번째가 최저가

        Instant now = Instant.now();

        IngredientPriceResponse response = IngredientPriceResponse.builder()
                .ingredientId(null)
                .ingredientName(keyword)
                .lowestPrice(lowestPrice)
                .items(allItems)
                .cachedAt(now)
                .expiresAt(now.plus(CACHE_HOURS, ChronoUnit.HOURS))
                .build();

        // 3. Redis 캐시 저장
        cacheToRedis(KEYWORD_PREFIX + normalizedKeyword, response);

        return response;
    }
}

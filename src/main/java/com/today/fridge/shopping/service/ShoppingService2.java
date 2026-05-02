package com.today.fridge.shopping.service;

import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.entity.ShoppingItem;
import com.today.fridge.shopping.exception.ErrorCode2;
import com.today.fridge.shopping.external.coupang.CoupangShoppingClient2;
import com.today.fridge.shopping.external.naver.NaverShoppingClient;
import com.today.fridge.shopping.repository.ShoppingItemRepository;
import com.today.fridge.shopping.type.StockStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingService2 {
    // DB에 1시간동안 저장
    private static final long CACHE_HOURS = 1;

    private final ShoppingItemRepository shoppingItemRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final NaverShoppingClient naverClient;
    private final CoupangShoppingClient2 coupangClient;  // ElevenStShoppingClient(@Primary)가 주입됨

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public IngredientPriceResponse getIngredientPrices(Long ingredientMasterId) {
        IngredientMaster master = ingredientMasterRepository.findById(ingredientMasterId)
                .orElseThrow(ErrorCode2.INGREDIENT_MASTER_NOT_FOUND::toException);

        return fetchPrices(master);
    }

    @Transactional
    public List<IngredientPriceResponse> getFridgePrices(Long userId) {
        List<IngredientMaster> masters = em.createQuery(
                        "SELECT DISTINCT ui.ingredientMaster FROM UserIngredient ui " +
                        "WHERE ui.user.userId = :userId AND ui.ingredientMaster IS NOT NULL",
                        IngredientMaster.class)
                .setParameter("userId", userId)
                .getResultList();

        if (masters.isEmpty()) {
            return List.of();
        }

        return masters.stream()
                .map(this::fetchPrices)
                .toList();
    }

    // fetchPrices: 1. DB 캐시 확인 → 2. 없으면 네이버+11번가 병렬 검색 → 3. 가격순 정렬 → 4. 1시간 캐시 저장
    private IngredientPriceResponse fetchPrices(IngredientMaster master) {
        Instant now = Instant.now();

        List<ShoppingItem> cached = shoppingItemRepository
                .findByIngredientMaster_IngredientMasterIdAndExpiresAtAfterOrderByPriceAsc(
                        master.getIngredientMasterId(), now);

        if (!cached.isEmpty()) {
            return buildResponse(master, cached);
        }

        String keyword = master.getCanonicalName();

        // 네이버와 11번가에 동시에 비동기 요청하여 속도를 높임
        CompletableFuture<List<ShoppingItemDto>> naverFuture =
                CompletableFuture.supplyAsync(() -> naverClient.search(keyword));
        CompletableFuture<List<ShoppingItemDto>> elevenStFuture =
                CompletableFuture.supplyAsync(() -> coupangClient.search(keyword));

        List<ShoppingItemDto> naverItems = naverFuture.join();
        List<ShoppingItemDto> elevenStItems = elevenStFuture.join();

        List<ShoppingItemDto> allItems = Stream.concat(naverItems.stream(), elevenStItems.stream())
                .sorted((a, b) -> Integer.compare(a.getPrice(), b.getPrice()))
                .toList();

        if (allItems.isEmpty()) {
            throw ErrorCode2.ALL_SHOPPING_API_FAILED.toException();
        }

        Instant expiresAt = now.plus(CACHE_HOURS, ChronoUnit.HOURS);
        List<ShoppingItem> toSave = allItems.stream()
                .map(dto -> toEntity(dto, master, now, expiresAt))
                .toList();

        shoppingItemRepository.saveAll(toSave);
        cleanupExpiredAsync(now);

        return buildResponse(master, toSave);
    }

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

    // 백그라운드에서 만료된 캐시 자동 정리
    private void cleanupExpiredAsync(Instant now) {
        CompletableFuture.runAsync(() -> {
            try {
                shoppingItemRepository.deleteExpired(now);
            } catch (Exception e) {
                log.warn("[ShoppingService2] 만료 캐시 삭제 실패: {}", e.getMessage());
            }
        });
    }
}

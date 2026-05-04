package com.today.fridge.shopping.controller;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.ApiResponse; 
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.service.ShoppingService3;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis 캐시 기반 쇼핑 컨트롤러 (ShoppingController2의 Redis 적용 버전).
 *
 * 추가: /api/v1/shopping/search?keyword=계란 — 키워드 기반 실시간 최저가 검색
 */
@RestController
@RequestMapping("/api/v1/shopping")
@RequiredArgsConstructor
public class ShoppingController {

    private final ShoppingService3 shoppingService3;

    /**
     * 식재료 ID로 최저가 조회 (Redis 캐시 우선)
     */
    @GetMapping("/ingredients/{ingredientId}/prices")
    public ResponseEntity<ApiResponse<IngredientPriceResponse>> getIngredientPrices(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId) {
        requireUserId(userId);
        IngredientPriceResponse data = shoppingService3.getIngredientPrices(ingredientId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 최저가 조회 성공"));
    }

    /**
     * 냉장고 식재료 전체 최저가 조회
     */
    @GetMapping("/fridge/prices")
    public ResponseEntity<ApiResponse<List<IngredientPriceResponse>>> getFridgePrices(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        long uid = requireUserId(userId);
        List<IngredientPriceResponse> data = shoppingService3.getFridgePrices(uid);
        return ResponseEntity.ok(ApiResponse.success(data, "냉장고 식재료 최저가 조회 성공"));
    }

    /**
     * 키워드 기반 실시간 최저가 검색.
     * DB의 ingredient_master에 없는 식재료도 직접 검색 가능합니다. -> 추후 냉장고에 재료가 있다면 냉장고에 재료가 있다는걸 알려주는 기능 도입 검토중
     *
     * GET /api/v1/shopping/search?keyword=계란
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<IngredientPriceResponse>> searchByKeyword(
            @RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", "검색어를 입력해주세요."));
        }
        IngredientPriceResponse data = shoppingService3.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(data, "실시간 최저가 검색 성공"));
    }

    private static long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

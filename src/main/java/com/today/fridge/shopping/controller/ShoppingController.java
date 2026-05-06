package com.today.fridge.shopping.controller;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.service.ShoppingService3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis 캐시 기반 쇼핑 컨트롤러 (ShoppingController2의 Redis 적용 버전).
 *
 * 추가: /api/v1/shopping/search?keyword=계란 — 키워드 기반 실시간 최저가 검색
 */
@Tag(name = "Shopping", description = "식재료 최저가 조회 API")
@RestController
@RequestMapping("/api/v1/shopping")
@RequiredArgsConstructor
public class ShoppingController {

    private final ShoppingService3 shoppingService3;

    /**
     * 식재료 ID로 최저가 조회 (Redis 캐시 우선)
     */
    @Operation(summary = "식재료 ID 최저가 조회", description = "식재료 ID로 네이버/11번가/쿠팡 최저가를 조회합니다. Redis 캐시(1시간) 우선 사용.")
    @GetMapping("/ingredients/{ingredientId}/prices")
    public ResponseEntity<ApiResponse<IngredientPriceResponse>> getIngredientPrices(
            @Parameter(description = "사용자 ID (JWT 인증 헤더에서 자동 주입)", example = "1")
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(description = "식재료 ID", example = "1")
            @Parameter(description = "ingredientId") @PathVariable("ingredientId") Long ingredientId) {
        requireUserId(userId);
        IngredientPriceResponse data = shoppingService3.getIngredientPrices(ingredientId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 최저가 조회 성공"));
    }

    /**
     * 냉장고 식재료 전체 최저가 조회
     */
    @Operation(summary = "냉장고 식재료 전체 최저가 조회", description = "로그인한 사용자의 냉장고에 있는 모든 식재료의 최저가를 조회합니다.")
    @GetMapping("/fridge/prices")
    public ResponseEntity<ApiResponse<List<IngredientPriceResponse>>> getFridgePrices(
            @Parameter(description = "사용자 ID (JWT 인증 헤더에서 자동 주입)", example = "1")
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId) {
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
    @Operation(summary = "키워드 실시간 최저가 검색", description = "키워드로 네이버/11번가/쿠팡에서 실시간 최저가를 검색합니다. 인증 불필요.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<IngredientPriceResponse>> searchByKeyword(
            @Parameter(description = "검색할 식재료 키워드", example = "계란")
            @Parameter(description = "keyword") @RequestParam("keyword") String keyword) {
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

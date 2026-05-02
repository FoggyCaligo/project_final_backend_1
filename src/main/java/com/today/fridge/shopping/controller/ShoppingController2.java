package com.today.fridge.shopping.controller;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.shopping.dto.IngredientPriceResponse;
import com.today.fridge.shopping.service.ShoppingService2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/shopping")
@RequiredArgsConstructor
public class ShoppingController2 {

    private final ShoppingService2 shoppingService2;

    @GetMapping("/ingredients/{ingredientId}/prices")
    public ResponseEntity<ApiResponse<IngredientPriceResponse>> getIngredientPrices(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId) {
        requireUserId(userId);
        IngredientPriceResponse data = shoppingService2.getIngredientPrices(ingredientId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 최저가 조회 성공"));
    }

    @GetMapping("/fridge/prices")
    public ResponseEntity<ApiResponse<List<IngredientPriceResponse>>> getFridgePrices(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        long uid = requireUserId(userId);
        List<IngredientPriceResponse> data = shoppingService2.getFridgePrices(uid);
        return ResponseEntity.ok(ApiResponse.success(data, "냉장고 식재료 최저가 조회 성공"));
    }

    private static long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

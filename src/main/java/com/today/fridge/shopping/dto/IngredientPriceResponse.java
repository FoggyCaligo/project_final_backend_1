package com.today.fridge.shopping.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class IngredientPriceResponse {

    private Long ingredientId;
    private String ingredientName;
    private Integer lowestPrice;
    private List<ShoppingItemDto> items;
    private Instant cachedAt;
    private Instant expiresAt;
}

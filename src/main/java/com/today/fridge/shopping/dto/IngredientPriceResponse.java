package com.today.fridge.shopping.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Builder
public class IngredientPriceResponse {

    private Long ingredientId;
    private String ingredientName;
    private Integer lowestPrice;
    private List<ShoppingItemDto> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant cachedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant expiresAt;
}

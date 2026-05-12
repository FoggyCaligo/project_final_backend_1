package com.today.fridge.shopping.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Builder(toBuilder = true)
public class IngredientPriceResponse {

    private Long ingredientId;
    private String ingredientName;
    private Integer lowestPrice;
    private List<ShoppingItemDto> items;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String explanation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant cachedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant expiresAt;
}

package com.today.fridge.ingredient.dto;

import com.today.fridge.global.response.PageResponse;

import java.util.List;

public record FridgeIngredientListData(List<IngredientResponse> items, PageResponse pageInfo) {
}

package com.today.fridge.ingredient.dto;

import com.today.fridge.global.response.PageInfo;

import java.util.List;

public record FridgeIngredientListData(List<FridgeIngredientItemResponse> items, PageInfo pageInfo) {
}

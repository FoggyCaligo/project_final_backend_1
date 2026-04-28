package com.today.fridge.ingredient.dto;

import java.util.List;

/** 지침서 v1.0 GET /summary data */
public record FridgeSummaryResponse(
        long totalCount,
        long freshCount,
        long soonCount,
        long expiredCount,
        List<SoonItemResponse> soonItems
) {
}

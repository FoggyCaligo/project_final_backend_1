package com.today.fridge.shopping.external.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverShoppingResponse(
        Integer total,
        Integer start,
        Integer display,
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String title,
            String link,
            String image,
            String lprice,
            String hprice,
            String mallName,
            String productId,
            String brand,
            String maker,
            String category1,
            String category2,
            String deliveryFee   // 배송비 (0이면 무료배송)
    ) {}
}

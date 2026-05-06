package com.today.fridge.shopping.external.coupang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoupangSearchResponse(
        String rCode,
        String rMessage,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Integer totalCount,
            List<ProductData> productData
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductData(
            Long itemId,
            Long vendorItemId,
            String productName,
            Integer unitPrice,
            Integer originalPrice,
            Integer discountRate,
            String productUrl,
            String productImage,
            Boolean isRocket
    ) {}
}

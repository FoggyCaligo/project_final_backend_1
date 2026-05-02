package com.today.fridge.shopping.dto;

import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ShoppingItemDto {

    private String mallName;
    private String mallProductId;
    private String productName;
    private String brand;
    private Integer price;
    private Integer originalPrice;
    private BigDecimal discountRate;
    private String purchaseUrl;
    private String imageUrl;
    private ShippingType shippingType;
    private StockStatus stockStatus;
}

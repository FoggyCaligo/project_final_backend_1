package com.today.fridge.shopping.entity;

import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shopping_item_mcp", schema = "today_fridge")
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_item_id")
    private Long shoppingItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_master_id", nullable = false)
    private IngredientMaster ingredientMaster;

    @Column(name = "mall_name", nullable = false, length = 50)
    private String mallName;

    @Column(name = "mall_product_id", length = 100)
    private String mallProductId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "package_weight", precision = 10, scale = 2)
    private BigDecimal packageWeight;

    @Column(name = "package_unit", length = 10)
    private String packageUnit;

    @Column(name = "unit_price_per_100", precision = 10, scale = 2)
    private BigDecimal unitPricePer100;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_type")
    private ShippingType shippingType;

    @Column(name = "shipping_fee", nullable = false)
    @Builder.Default
    private Integer shippingFee = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "purchase_url", nullable = false, columnDefinition = "TEXT")
    private String purchaseUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        if (updatedAt == null) updatedAt = Instant.now();
        if (fetchedAt == null) fetchedAt = Instant.now();
    }
}

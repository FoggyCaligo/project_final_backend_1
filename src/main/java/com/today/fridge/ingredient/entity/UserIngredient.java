package com.today.fridge.ingredient.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import com.today.fridge.ingredient.domain.FreshnessCalculator;
import com.today.fridge.ingredient.type.FreshnessStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_ingredient")
public class UserIngredient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_ingredient_id")
    private Long userIngredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_master_id")
    private IngredientMaster ingredientMaster;

    @Column(name = "raw_name", length = 100)
    private String rawName;

    @Column(name = "normalized_name_snapshot", length = 100)
    private String normalizedNameSnapshot;

    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "storage_type", length = 30)
    private String storageType;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "freshness_status", length = 20)
    private FreshnessStatus freshnessStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(KST);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        refreshFreshness();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(KST);
        refreshFreshness();
    }

    public void refreshFreshness() {
        LocalDate today = LocalDate.now(KST);
        this.freshnessStatus = FreshnessCalculator.computeStatus(expiresAt, today);
    }
}
package com.today.fridge.ingredient.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_ingredient")
public class UserIngredient {

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

    @Column(name = "freshness_status", length = 20)
    private String freshnessStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
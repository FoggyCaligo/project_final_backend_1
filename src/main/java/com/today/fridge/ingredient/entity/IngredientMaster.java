package com.today.fridge.ingredient.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ingredient_master")
public class IngredientMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_master_id")
    private Long ingredientMasterId;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private IngredientCategory category;

    @Column(name = "alias_text", columnDefinition = "TEXT")
    private String aliasText;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
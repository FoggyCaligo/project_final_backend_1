package com.todayfridge.backend1.domain.recipe.entity;

import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "recipe_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Recipe recipe;
    @Column(nullable = false, length = 120)
    private String ingredientName;
    @Column(length = 120)
    private String normalizedName;
    @Column(length = 100)
    private String amountText;

    protected RecipeIngredient() {}

    public String getIngredientName() { return ingredientName; }
    public String getNormalizedName() { return normalizedName; }
    public String getAmountText() { return amountText; }
}

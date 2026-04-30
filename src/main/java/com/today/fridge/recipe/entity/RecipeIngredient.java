package com.today.fridge.recipe.entity;

import com.today.fridge.ingredient.entity.IngredientMaster;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_ingredient_id")
    private Long recipeIngredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_master_id")
    private IngredientMaster ingredientMaster;

    @Column(name = "raw_text", length = 200)
    private String rawText;

    @Column(name = "normalized_name_snapshot", length = 100)
    private String normalizedNameSnapshot;

    @Column(name = "amount_text", length = 50)
    private String amountText;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "is_optional")
    private Boolean isOptional;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
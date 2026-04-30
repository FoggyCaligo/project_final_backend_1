package com.today.fridge.recommendation.entity;

import com.today.fridge.recipe.entity.Recipe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "recipe_condition_map",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_recipe_condition",
                        columnNames = {"recipe_id", "condition_id"}
                )
        }
)
public class RecipeConditionMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_condition_map_id")
    private Long recipeConditionMapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private ConditionCode conditionCode;

    @Column(name = "fit_type", length = 20)
    private String fitType; // RECOMMENDED, ALLOWED, CAUTION

    @Column(name = "source_type", length = 20)
    private String sourceType; // MANUAL, RULE, INFERRED

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;
    
    public static RecipeConditionMap create(
            Recipe recipe,
            ConditionCode conditionCode,
            String fitType,
            String sourceType,
            BigDecimal confidenceScore
    ) {
        RecipeConditionMap map = new RecipeConditionMap();
        map.recipe = recipe;
        map.conditionCode = conditionCode;
        map.fitType = fitType;
        map.sourceType = sourceType;
        map.confidenceScore = confidenceScore;
        return map;
    }

    public void updateAnalysis(
            String fitType,
            String sourceType,
            BigDecimal confidenceScore
    ) {
        this.fitType = fitType;
        this.sourceType = sourceType;
        this.confidenceScore = confidenceScore;
    }
}
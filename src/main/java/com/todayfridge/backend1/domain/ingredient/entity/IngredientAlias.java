package com.todayfridge.backend1.domain.ingredient.entity;

import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "ingredient_aliases")
public class IngredientAlias extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String aliasName;
    @JoinColumn(name = "ingredient_master_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private IngredientMaster ingredientMaster;

    protected IngredientAlias() {}
    public IngredientAlias(String aliasName, IngredientMaster ingredientMaster) {
        this.aliasName = aliasName;
        this.ingredientMaster = ingredientMaster;
    }
}

package com.todayfridge.backend1.domain.ingredient.entity;

import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "ingredient_master")
public class IngredientMaster extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String normalizedName;
    @Column(nullable = false, length = 50)
    private String category;

    protected IngredientMaster() {}
    public IngredientMaster(String normalizedName, String category) {
        this.normalizedName = normalizedName;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getNormalizedName() { return normalizedName; }
    public String getCategory() { return category; }
}

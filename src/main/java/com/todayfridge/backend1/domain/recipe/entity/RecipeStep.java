package com.todayfridge.backend1.domain.recipe.entity;

import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "recipe_steps")
public class RecipeStep extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "recipe_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Recipe recipe;
    @Column(nullable = false)
    private Integer stepOrder;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(length = 500)
    private String imageUrl;

    protected RecipeStep() {}

    public Integer getStepOrder() { return stepOrder; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
}

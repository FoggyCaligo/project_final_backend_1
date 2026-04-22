package com.today.fridge.recipe.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "source_site", length = 50)
    private String sourceSite;

    @Column(name = "source_recipe_key", length = 100)
    private String sourceRecipeKey;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "servings_text", length = 50)
    private String servingsText;

    @Column(name = "cook_time_text", length = 50)
    private String cookTimeText;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
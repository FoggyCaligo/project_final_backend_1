package com.todayfridge.backend1.domain.recipe.entity;

import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "recipes")
public class Recipe extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 80)
    private String sourceRecipeId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 500)
    private String thumbnailUrl;
    @Column(length = 1000)
    private String summary;

    protected Recipe() {}

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getSummary() { return summary; }
}

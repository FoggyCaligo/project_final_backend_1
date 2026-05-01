package com.today.fridge.recipe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_tag",
       indexes = {
           @Index(name = "idx_recipe_tag_recipe", columnList = "recipe_id"),
           @Index(name = "idx_recipe_tag_code", columnList = "tag_code")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private RecipeTagType tagType;

    @Column(name = "tag_code", nullable = false, length = 50)
    private String tagCode; // SOUP, STEW, SPICY ...

    @Column(name = "confidence")
    private Double confidence; // 0.0 ~ 1.0

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private RecipeTagSourceType sourceType;
}
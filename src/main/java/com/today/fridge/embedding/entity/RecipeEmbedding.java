package com.today.fridge.embedding.entity;

import java.time.LocalDateTime;

import com.today.fridge.recipe.entity.Recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recipe_embedding",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_recipe_embedding_recipe_model",
            columnNames = {"recipe_id", "model_name"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 원본 레시피
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    // 임베딩에 사용한 원문
    @Column(name = "embedding_text",nullable = false, columnDefinition = "TEXT")
    private String embeddingText;

    // pgvector 컬럼
    @Column(nullable = false,columnDefinition = "vector(384)")
    private String embedding;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at" ,nullable = false)
    private LocalDateTime createdAt;

    public static RecipeEmbedding create(
            Recipe recipe,
            String embeddingText,
            String embedding,
            String modelName
    ) {
        RecipeEmbedding e = new RecipeEmbedding();
        e.recipe = recipe;
        e.embeddingText = embeddingText;
        e.embedding = embedding;
        e.modelName = modelName;
        e.isActive = true;
        e.createdAt = LocalDateTime.now();
        return e;
    }
}
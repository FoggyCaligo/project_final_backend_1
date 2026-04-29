package com.today.fridge.embedding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.today.fridge.embedding.entity.RecipeEmbedding;

public interface RecipeEmbeddingRepository
        extends JpaRepository<RecipeEmbedding, Long> {

    @Query(value = """
        select recipe_id
        from recipe_embedding
        where is_active = true
        order by embedding <=> cast(:queryVector as vector)
        limit :limit
        """,
        nativeQuery = true)
    List<Long> findSimilarRecipeIds(
            String queryVector,
            int limit
    );
}
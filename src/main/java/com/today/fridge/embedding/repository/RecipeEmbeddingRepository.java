package com.today.fridge.embedding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.embedding.dto.SemanticSearchResult;
import com.today.fridge.embedding.entity.RecipeEmbedding;
//import com.today.fridge.recipe.entity.Recipe;

public interface RecipeEmbeddingRepository
                extends JpaRepository<RecipeEmbedding, Long> {

	@Query(value = """
			select recipe_id as recipeId,
			       embedding <=> cast(:queryVector as vector) as distance
			from today_fridge.recipe_embedding
			where is_active = true
			  and model_name = :modelName
			order by distance
			limit :limit
			""", nativeQuery = true)
        List<SemanticSearchResult> findSimilarRecipes(
                        @Param("queryVector") String queryVector,
                        @Param("modelName") String modelName,
                        @Param("limit") Integer limit);

        boolean existsByRecipeRecipeIdAndModelName(
                        Long recipeId,
                        String modelName);

        @Modifying
        @Query(value = """
                        insert into recipe_embedding
                        (
                         recipe_id,
                         embedding_text,
                         embedding,
                         model_name,
                         is_active,
                         created_at
                        )
                        values
                        (
                         :recipeId,
                         :embeddingText,
                         cast(:embedding as vector),
                         :modelName,
                         true,
                         now()
                        )
                        """, nativeQuery = true)
        void insertEmbedding(
                        @Param("recipeId") Long recipeId,
                        @Param("embeddingText") String embeddingText,
                        @Param("embedding") String embedding,
                        @Param("modelName") String modelName);

        Optional<RecipeEmbedding> findByRecipe_RecipeIdAndIsActiveTrue(Long recipeId);
}
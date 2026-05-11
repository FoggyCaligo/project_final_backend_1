package com.today.fridge.recipe.repository;

/*
 * 이 리포지토리에서는 Recipe 객체의 Recipe_Id를 사용하여 모든 재료 정보를 반환받습니다.
 */

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recipe.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipe_RecipeIdOrderBySortOrderAsc(Long recipeId);

	@Query("""
		    select coalesce(im.normalizedName, ri.normalizedNameSnapshot, ri.rawText)
		    from RecipeIngredient ri
		    left join ri.ingredientMaster im
		    where ri.recipe.recipeId = :recipeId
		      and (ri.isOptional = false or ri.isOptional is null)
		    order by ri.sortOrder asc
		""")
		List<String> findRequiredIngredientNamesByRecipeId(
		    @Param("recipeId") Long recipeId
		);
	@Query("""
			select distinct ri.normalizedNameSnapshot
			from RecipeIngredient ri
			where ri.normalizedNameSnapshot is not null
			""")
			List<String> findDistinctIngredientNames();
	
	interface RecipeIngredientNameRow {
        Long getRecipeId();
        String getIngredientName();
    }

    @Query("""
        select ri.recipe.recipeId as recipeId,
               ri.normalizedNameSnapshot as ingredientName
        from RecipeIngredient ri
        where ri.recipe.recipeId in :recipeIds
    """)
    List<RecipeIngredientNameRow> findRequiredIngredientNamesByRecipeIds(
            @Param("recipeIds") List<Long> recipeIds
    );
}

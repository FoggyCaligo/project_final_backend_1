package com.today.fridge.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recipe.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

	@Query("""
		    select ri.normalizedNameSnapshot
		    from RecipeIngredient ri
		    where ri.recipe.recipeId = :recipeId
		      and ri.normalizedNameSnapshot is not null
		      and (ri.isOptional = false or ri.isOptional is null)
		    order by ri.sortOrder asc
		""")
		List<String> findRequiredIngredientNamesByRecipeId(
		    @Param("recipeId") Long recipeId
		);
}

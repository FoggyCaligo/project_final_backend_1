package com.today.fridge.recipe.repository;

/*
 * RecipeNutritionRepositoryTest는 RecipeNutritionRepository의 쿼리 메서드를 테스트하는 클래스입니다.
 *
 * 주요 테스트:
 * - findByRecipe_RecipeId: 레시피 ID로 해당 레시피의 영양정보를 정상적으로 조회하는지 확인
 */

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("recipe")
class RecipeNutritionRepositoryTest {

	@Autowired
	private RecipeRepository recipeRepository;

	@Autowired
	private RecipeNutritionRepository recipeNutritionRepository;

	@Test
	@DisplayName("UT-RECIPE-02 - 레시피 ID로 영양정보 조회")
	void testFindByRecipe_RecipeId() {
		// Given
		Recipe recipe = Recipe.builder()
				.sourceSite("test.com")
				.sourceRecipeKey("test")
				.title("Test Recipe")
				.thumbnailUrl("test.jpg")
				.summary("This is a test recipe")
				.servingsText("1")
				.cookTimeText("10분")
				.sourceUrl("test.com")
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
		Recipe savedRecipe = recipeRepository.save(recipe);

		RecipeNutrition nutrition = RecipeNutrition.builder()
				.recipe(savedRecipe)
				.calories(BigDecimal.valueOf(500))
				.sodium(BigDecimal.valueOf(100))
				.carbs(BigDecimal.valueOf(100))
				.sugar(BigDecimal.valueOf(100))
				.protein(BigDecimal.valueOf(100))
				.fat(BigDecimal.valueOf(100))
				.build();
		recipeNutritionRepository.save(nutrition);

		// When
		Optional<RecipeNutrition> foundNutrition = recipeNutritionRepository
				.findByRecipe_RecipeId(savedRecipe.getRecipeId());

		// Then
		assertThat(foundNutrition).isPresent();
		assertThat(foundNutrition.get().getCalories()).isEqualByComparingTo(BigDecimal.valueOf(500));
		assertThat(foundNutrition.get().getProtein()).isEqualByComparingTo(BigDecimal.valueOf(100));
	}
}
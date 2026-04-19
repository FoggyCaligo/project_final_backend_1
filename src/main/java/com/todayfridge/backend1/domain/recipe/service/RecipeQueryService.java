package com.todayfridge.backend1.domain.recipe.service;

import com.todayfridge.backend1.domain.recipe.entity.Recipe;
import com.todayfridge.backend1.domain.recipe.repository.RecipeIngredientRepository;
import com.todayfridge.backend1.domain.recipe.repository.RecipeRepository;
import com.todayfridge.backend1.domain.recipe.repository.RecipeStepRepository;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecipeQueryService {
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;

    public RecipeQueryService(RecipeRepository recipeRepository, RecipeIngredientRepository recipeIngredientRepository,
                              RecipeStepRepository recipeStepRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeStepRepository = recipeStepRepository;
    }

    public List<Map<String, Object>> search(String keyword) {
        List<Recipe> recipes = (keyword == null || keyword.isBlank())
                ? recipeRepository.findTop50ByOrderByIdDesc()
                : recipeRepository.findByTitleContainingIgnoreCase(keyword);
        return recipes.stream().map(this::toSummary).toList();
    }

    public Map<String, Object> detail(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다."));
        Map<String, Object> result = new LinkedHashMap<>(toSummary(recipe));
        result.put("ingredients", recipeIngredientRepository.findByRecipe_Id(recipeId).stream().map(i -> Map.of(
                "ingredientName", i.getIngredientName(),
                "normalizedName", i.getNormalizedName(),
                "amountText", i.getAmountText()
        )).toList());
        result.put("steps", recipeStepRepository.findByRecipe_IdOrderByStepOrderAsc(recipeId).stream().map(s -> Map.of(
                "stepOrder", s.getStepOrder(),
                "description", s.getDescription(),
                "imageUrl", s.getImageUrl()
        )).toList());
        return result;
    }

    private Map<String, Object> toSummary(Recipe recipe) {
        return Map.of(
                "recipeId", recipe.getId(),
                "title", recipe.getTitle(),
                "thumbnailUrl", recipe.getThumbnailUrl(),
                "summary", recipe.getSummary()
        );
    }
}

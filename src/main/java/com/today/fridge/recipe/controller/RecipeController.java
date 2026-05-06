package com.today.fridge.recipe.controller;

/*
 * 상세 레시피와 관련된 Controller
 * 기능:
 * 1. 모든 레시피 조회
 *      - 페이징을 사용하여 현재 12개씩 조회 (변경 가능)
 *      - @PageableDefault 사용
 *      - 성공 시 "전체 레시피 조회 성공"
 * 2. 상세 레시피 조회
 *      - 레시피 ID를 사용하여 조회
 *      - @PathVariable 사용
 *      - 성공 시 "상세 레시피 조회 성공"
 * 3. 레시피 조리 완료
 *      - 레시피 ID를 사용하여 조회
 *      - @PathVariable 사용
 *      - 성공 시 "레시피 조리 완료"
 */

// Pageable
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

// Response Entity & API Response
import org.springframework.http.ResponseEntity;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.global.response.PageResult;

// Spring Framework
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// DTO
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.dto.response.RecipeResponse;

// Service
import com.today.fridge.recipe.service.RecipeService;

// Lombok
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

	private final RecipeService recipeService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResult<RecipeListResponse>>> getRecipes(
			@RequestParam(name = "cookingType", required = false, defaultValue = "ALL") String cookingType,
			@RequestParam(name = "sort", required = false, defaultValue = "default") String sort,
			@PageableDefault(size = 12) Pageable pageable) {
		return ResponseEntity.ok(
				ApiResponse.success(
						recipeService.getRecipes(cookingType, sort, pageable),
						"전체 레시피 조회 성공"));
	}

	@GetMapping("/{recipeId}")
	public ResponseEntity<ApiResponse<RecipeResponse>> getRecipe(
			@PathVariable("recipeId") Long recipeId,
			@RequestHeader(value = "X-User-Id", required = false) Long userId) {
		return ResponseEntity.ok(
				ApiResponse.success(
						recipeService.getRecipe(recipeId, userId),
						"상세 레시피 조회 성공"));
	}

	@PostMapping("/{recipeId}/cooked")
	public ResponseEntity<ApiResponse<Void>> ateRecipe(
			@PathVariable("recipeId") Long recipeId,
			@RequestHeader(value = "X-User-Id", required = false) Long userId) {
		recipeService.ateRecipe(recipeId, userId);
		return ResponseEntity.ok(
				ApiResponse.success(
						null,
						"레시피 재료 소진"));
	}

}

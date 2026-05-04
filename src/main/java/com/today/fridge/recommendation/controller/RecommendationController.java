package com.today.fridge.recommendation.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;
	
	@GetMapping("/recommendations")
	public ResponseEntity<ApiResponse<PageResult<RecipeRecommendationResponse>>> recommend(
	        @PageableDefault(size = 9) Pageable pageable
	) {
		// TODO : user 연결 해야
		Long mockUserId = 1L;
		
		PageResult<RecipeRecommendationResponse> result =
	            recommendationService.recommend(mockUserId, pageable);

	    return ResponseEntity.ok(ApiResponse.success(result, "추천 레시피 조회 성"));
	}
}

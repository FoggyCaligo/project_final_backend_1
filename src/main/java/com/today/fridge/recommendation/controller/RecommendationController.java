package com.today.fridge.recommendation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;
	
	@GetMapping("/recommendations")
	public ResponseEntity<ApiResponse<List<RecipeRecommendationResponse>>> recommend() {
		// TODO : user 연결
		Long mockUserId = 1L;
		
		List<RecipeRecommendationResponse> result =
	            recommendationService.recommend(mockUserId);

	    return ResponseEntity.ok(ApiResponse.success(result, "추천 레시피 조회 성"));
	}
}

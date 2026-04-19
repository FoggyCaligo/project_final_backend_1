package com.todayfridge.backend1.domain.recipe.controller;

import com.todayfridge.backend1.domain.recipe.service.RecommendationService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recipes/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> recommend(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(recommendationService.recommend(loginUser.getUserId()));
    }
}

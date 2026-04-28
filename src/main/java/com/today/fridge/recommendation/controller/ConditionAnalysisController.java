package com.today.fridge.recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.recommendation.service.RecipeConditionAnalyzeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/recommendation")
@RequiredArgsConstructor
public class ConditionAnalysisController {

    private final RecipeConditionAnalyzeService recipeConditionAnalyzeService;

    @PostMapping("/recipes/{recipeId}/condition-analysis")
    public ResponseEntity<ApiResponse<Void>> analyze(
            @PathVariable("recipeId") Long recipeId
    ) {

        recipeConditionAnalyzeService.saveDummyAnalysis(recipeId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "조건 분석 저장 완료"
                )
        );
    }
}

package com.today.fridge.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.recommendation.service.RecipeConditionAnalyzeService;

import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "ConditionAnalysis", description = "ConditionAnalysisController API")
@RequestMapping("/api/v1/admin/recommendation")
@RequiredArgsConstructor
public class ConditionAnalysisController {

    private final RecipeConditionAnalyzeService recipeConditionAnalyzeService;

    @PostMapping("/recipes/{recipeId}/condition-analysis")
    @Operation(summary = "ConditionAnalysis API")
    public ResponseEntity<ApiResponse<Void>> analyze(
            @Parameter(description = "recipeId") @PathVariable("recipeId") Long recipeId
    ) {

        recipeConditionAnalyzeService.analyzeAndSave(recipeId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "임베딩 기반 조건 분석 저장 완료"
                )
        );
    }
    @PostMapping("/recipes/condition-analysis/bulk")
    @Operation(summary = "ConditionAnalysis API")
    public ResponseEntity<ApiResponse<Void>> analyzeAll() {

        recipeConditionAnalyzeService.analyzeAllRecipes();

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "전체 레시피 조건 분석 완료"
                )
        );
    }
}

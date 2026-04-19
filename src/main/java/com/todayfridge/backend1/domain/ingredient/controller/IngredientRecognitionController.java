package com.todayfridge.backend1.domain.ingredient.controller;

import com.todayfridge.backend1.domain.ingredient.service.IngredientRecognitionService;
import com.todayfridge.backend1.domain.ingredient.service.IngredientService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/fridge/ingredients")
public class IngredientRecognitionController {
    private final IngredientRecognitionService ingredientRecognitionService;
    private final IngredientService ingredientService;

    public IngredientRecognitionController(IngredientRecognitionService ingredientRecognitionService, IngredientService ingredientService) {
        this.ingredientRecognitionService = ingredientRecognitionService;
        this.ingredientService = ingredientService;
    }

    @PostMapping("/recognize-image")
    public ApiResponse<Map<String, Object>> recognize(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(ingredientRecognitionService.recognize(file));
    }

    @PostMapping("/recognize-image/confirm")
    public ApiResponse<Map<String, Object>> confirm(@CurrentUser LoginUser loginUser, @RequestBody ConfirmRequest request) {
        return ApiResponse.success("인식 결과를 반영해 등록했습니다.",
                ingredientService.create(loginUser.getUserId(), request.ingredientName(), request.quantity(), request.unit(),
                        request.expirationDate(), request.storageType()));
    }

    public record ConfirmRequest(String ingredientName, String quantity, String unit, LocalDate expirationDate, String storageType) {}
}

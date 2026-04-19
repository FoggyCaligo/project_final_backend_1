package com.todayfridge.backend1.domain.ingredient.controller;

import com.todayfridge.backend1.domain.ingredient.service.IngredientService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fridge")
public class FridgeIngredientController {
    private final IngredientService ingredientService;

    public FridgeIngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping("/ingredients")
    public ApiResponse<List<Map<String, Object>>> list(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(ingredientService.list(loginUser.getUserId()));
    }

    @PostMapping("/ingredients")
    public ApiResponse<Map<String, Object>> create(@CurrentUser LoginUser loginUser, @RequestBody IngredientRequest request) {
        return ApiResponse.success("식재료가 등록되었습니다.",
                ingredientService.create(loginUser.getUserId(), request.ingredientName(), request.quantity(),
                        request.unit(), request.expirationDate(), request.storageType()));
    }

    @PatchMapping("/ingredients/{ingredientId}")
    public ApiResponse<Map<String, Object>> update(@CurrentUser LoginUser loginUser,
                                                   @PathVariable Long ingredientId,
                                                   @RequestBody IngredientRequest request) {
        return ApiResponse.success("식재료가 수정되었습니다.",
                ingredientService.update(loginUser.getUserId(), ingredientId, request.ingredientName(),
                        request.quantity(), request.unit(), request.expirationDate(), request.storageType()));
    }

    @DeleteMapping("/ingredients/{ingredientId}")
    public ApiResponse<Void> delete(@CurrentUser LoginUser loginUser, @PathVariable Long ingredientId) {
        ingredientService.delete(loginUser.getUserId(), ingredientId);
        return ApiResponse.success("식재료가 삭제되었습니다.", null);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(ingredientService.summary(loginUser.getUserId()));
    }

    public record IngredientRequest(String ingredientName, String quantity, String unit, LocalDate expirationDate, String storageType) {}
}

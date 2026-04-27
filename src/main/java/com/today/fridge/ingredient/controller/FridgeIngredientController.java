package com.today.fridge.ingredient.controller;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.filter.RequestIdFilter;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.ingredient.dto.CategoryResponse;
import com.today.fridge.ingredient.dto.CreateIngredientRequest;
import com.today.fridge.ingredient.dto.DeleteIngredientData;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryResponse;
import com.today.fridge.ingredient.dto.IngredientResponse;
import com.today.fridge.ingredient.service.FridgeIngredientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fridge")
public class FridgeIngredientController {

    private final FridgeIngredientService fridgeIngredientService;

    public FridgeIngredientController(FridgeIngredientService fridgeIngredientService) {
        this.fridgeIngredientService = fridgeIngredientService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories(
            HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        List<CategoryResponse> data = fridgeIngredientService.listCategories();
        return ResponseEntity.ok(ApiResponse.ok(data, requestId, "카테고리 목록 조회 성공"));
    }

    @GetMapping("/ingredients")
    public ResponseEntity<ApiResponse<FridgeIngredientListData>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String freshnessStatus,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        long uid = requireUserId(userId);
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        FridgeIngredientListData data =
                fridgeIngredientService.list(uid, page, size, sort, freshnessStatus, storageType, keyword);
        return ResponseEntity.ok(ApiResponse.ok(data, requestId, "식재료 목록 조회 성공"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FridgeSummaryResponse>> summary(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        long uid = requireUserId(userId);
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        FridgeSummaryResponse data = fridgeIngredientService.summary(uid);
        return ResponseEntity.ok(ApiResponse.ok(data, requestId));
    }

    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CreateIngredientRequest body,
            HttpServletRequest request) {
        long uid = requireUserId(userId);
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        IngredientResponse created = fridgeIngredientService.create(uid, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, requestId, "식재료 등록 성공"));
    }

    @PatchMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientResponse>> patch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        long uid = requireUserId(userId);
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        IngredientResponse updated = fridgeIngredientService.patch(uid, ingredientId, body);
        return ResponseEntity.ok(ApiResponse.ok(updated, requestId, "식재료 수정 성공"));
    }

    @DeleteMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<DeleteIngredientData>> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            HttpServletRequest request) {
        long uid = requireUserId(userId);
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        DeleteIngredientData data = fridgeIngredientService.delete(uid, ingredientId);
        return ResponseEntity.ok(ApiResponse.ok(data, requestId, "식재료 삭제 성공"));
    }

    private static long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

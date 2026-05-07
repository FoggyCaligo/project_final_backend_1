package com.today.fridge.ingredient.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.ingredient.dto.CategoryResponse;
import com.today.fridge.ingredient.dto.CreateIngredientRequest;
import com.today.fridge.ingredient.dto.DeleteIngredientData;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryResponse;
import com.today.fridge.ingredient.dto.IngredientResponse;
import com.today.fridge.ingredient.dto.vision.VisionRecognizeDataDto;
import com.today.fridge.vision.dto.VisionRecognitionStatusDto;
import com.today.fridge.ingredient.service.FridgeIngredientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "FridgeIngredient", description = "FridgeIngredientController API")
@RequestMapping("/api/v1/fridge")
public class FridgeIngredientController {

    private final FridgeIngredientService fridgeIngredientService;

    public FridgeIngredientController(FridgeIngredientService fridgeIngredientService) {
        this.fridgeIngredientService = fridgeIngredientService;
    }

    @GetMapping("/categories")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories() {
        List<CategoryResponse> data = fridgeIngredientService.listCategories();
        return ResponseEntity.ok(ApiResponse.success(data, "카테고리 목록 조회 성공"));
    }

    @GetMapping("/ingredients/{ingredientId}")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<IngredientResponse>> getOne(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId) {
        long uid = requireUserId(userId);
        IngredientResponse data = fridgeIngredientService.getOne(uid, ingredientId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 조회 성공"));
    }

    @GetMapping("/ingredients")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<FridgeIngredientListData>> list(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "size") @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @Parameter(description = "freshnessStatus") @RequestParam(required = false) String freshnessStatus,
            @RequestParam(required = false) String storageType,
            @Parameter(description = "keyword") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        long uid = requireUserId(userId);
        FridgeIngredientListData data =
                fridgeIngredientService.list(
                        uid, page, size, sort, freshnessStatus, storageType, keyword, categoryId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 목록 조회 성공"));
    }

    @GetMapping("/summary")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<FridgeSummaryResponse>> summary(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        long uid = requireUserId(userId);
        FridgeSummaryResponse data = fridgeIngredientService.summary(uid);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping(value = "/ingredients/recognize-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<VisionRecognizeDataDto>> recognizeImage(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "topK") @RequestParam(defaultValue = "3") int topK) {
        long uid = requireUserId(userId);
        VisionRecognizeDataDto data = fridgeIngredientService.recognizeIngredientImage(uid, file, topK);
        return ResponseEntity.ok(ApiResponse.success(data, "이미지 인식 완료"));
    }

    @GetMapping("/ingredients/recognize-image/status/{requestId}")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<VisionRecognitionStatusDto>> recognitionStatus(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("requestId") Long requestId) {
        long uid = requireUserId(userId);
        VisionRecognitionStatusDto data = fridgeIngredientService.getRecognitionStatus(uid, requestId);
        return ResponseEntity.ok(ApiResponse.success(data, "인식 요청 조회 성공"));
    }

    @PostMapping("/ingredients")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<IngredientResponse>> create(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CreateIngredientRequest body) {
        long uid = requireUserId(userId);
        IngredientResponse created = fridgeIngredientService.create(uid, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "식재료 등록 성공"));
    }

    @PatchMapping("/ingredients/{ingredientId}")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<IngredientResponse>> patch(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            @RequestBody Map<String, Object> body) {
        long uid = requireUserId(userId);
        IngredientResponse updated = fridgeIngredientService.patch(uid, ingredientId, body);
        return ResponseEntity.ok(ApiResponse.success(updated, "식재료 수정 성공"));
    }

    @DeleteMapping("/ingredients/{ingredientId}")
    @Operation(summary = "FridgeIngredient API")
    public ResponseEntity<ApiResponse<DeleteIngredientData>> delete(
            @Parameter(description = "userId") @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId) {
        long uid = requireUserId(userId);
        DeleteIngredientData data = fridgeIngredientService.delete(uid, ingredientId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 삭제 성공"));
    }

    private static long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

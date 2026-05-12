package com.today.fridge.ingredient.controller;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.ingredient.dto.CategoryResponse;
import com.today.fridge.ingredient.dto.CreateIngredientRequest;
import com.today.fridge.ingredient.dto.DeleteIngredientData;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryResponse;
import com.today.fridge.ingredient.dto.IngredientResponse;
import com.today.fridge.ingredient.dto.RemoteImageStagingRequest;
import com.today.fridge.ingredient.dto.RemoteImageStagingResponse;
import com.today.fridge.ingredient.dto.RemoteImageStagingResultRequest;
import com.today.fridge.ingredient.dto.RemoteImageUploadResultRequest;
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
@RequestMapping("/api/v1/fridge")
public class FridgeIngredientController {

    private final FridgeIngredientService fridgeIngredientService;

    public FridgeIngredientController(FridgeIngredientService fridgeIngredientService) {
        this.fridgeIngredientService = fridgeIngredientService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories() {
        List<CategoryResponse> data = fridgeIngredientService.listCategories();
        return ResponseEntity.ok(ApiResponse.success(data, "카테고리 목록 조회 성공"));
    }

    @GetMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientResponse>> getOne(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId) {
        long uid = requireUserId(userId);
        IngredientResponse data = fridgeIngredientService.getOne(uid, ingredientId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 조회 성공"));
    }

    @GetMapping("/ingredients")
    public ResponseEntity<ApiResponse<FridgeIngredientListData>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(name = "page",defaultValue = "0") int page,
            @RequestParam(name = "size",defaultValue = "20") int size,
            @RequestParam(name = "sort",required = false) String sort,
            @RequestParam(name = "freshnessStatus",required = false) String freshnessStatus,
            @RequestParam(name = "storageType",required = false) String storageType,
            @RequestParam(name = "keyword",required = false) String keyword,
            @RequestParam(name = "categoryId",required = false) Long categoryId) {
        long uid = requireUserId(userId);
        FridgeIngredientListData data =
                fridgeIngredientService.list(
                        uid, page, size, sort, freshnessStatus, storageType, keyword, categoryId);
        return ResponseEntity.ok(ApiResponse.success(data, "식재료 목록 조회 성공"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FridgeSummaryResponse>> summary(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        long uid = requireUserId(userId);
        FridgeSummaryResponse data = fridgeIngredientService.summary(uid);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping(value = "/ingredients/recognize-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisionRecognizeDataDto>> recognizeImage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name="topK",defaultValue = "3") int topK) {
        long uid = requireUserId(userId);
        VisionRecognizeDataDto data = fridgeIngredientService.recognizeIngredientImage(uid, file, topK);
        return ResponseEntity.ok(ApiResponse.success(data, "이미지 인식 완료"));
    }

    @GetMapping("/ingredients/recognize-image/status/{requestId}")
    public ResponseEntity<ApiResponse<VisionRecognitionStatusDto>> recognitionStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("requestId") Long requestId) {
        long uid = requireUserId(userId);
        VisionRecognitionStatusDto data = fridgeIngredientService.getRecognitionStatus(uid, requestId);
        return ResponseEntity.ok(ApiResponse.success(data, "인식 요청 조회 성공"));
    }

    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CreateIngredientRequest body) {
        long uid = requireUserId(userId);
        IngredientResponse created = fridgeIngredientService.create(uid, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "식재료 등록 성공"));
    }

    @PatchMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientResponse>> patch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            @RequestBody Map<String, Object> body) {
        long uid = requireUserId(userId);
        IngredientResponse updated = fridgeIngredientService.patch(uid, ingredientId, body);
        return ResponseEntity.ok(ApiResponse.success(updated, "식재료 수정 성공"));
    }

    /** 아파치 업로드 성공/실패 반영 — 실패 시 {@code file_asset} 삭제 및 식재료 이미지 연결 해제 */
    @PatchMapping("/ingredients/{ingredientId}/apache-image-sync")
    public ResponseEntity<ApiResponse<IngredientResponse>> syncApacheImage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            @RequestBody RemoteImageUploadResultRequest body) {
        long uid = requireUserId(userId);
        IngredientResponse data = fridgeIngredientService.applyApacheImageSync(uid, ingredientId, body);
        return ResponseEntity.ok(ApiResponse.success(data, "아파치 이미지 동기화 반영"));
    }

    /** 식재료 이미지 교체: 새 {@code file_asset} 행만 생성(스테이징). 기존 {@code user_ingredient.file_id} 는 유지. */
    @PostMapping("/ingredients/{ingredientId}/apache-image-replace-staging")
    public ResponseEntity<ApiResponse<RemoteImageStagingResponse>> stageApacheImageReplace(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            @RequestBody RemoteImageStagingRequest body) {
        long uid = requireUserId(userId);
        RemoteImageStagingResponse data =
                fridgeIngredientService.stageApacheImageReplaceStaging(uid, ingredientId, body);
        return ResponseEntity.ok(ApiResponse.success(data, "이미지 교체 스테이징 생성"));
    }

    /** 스테이징 파일 아파치 업로드 후 성공 시 교체·실패 시 스테이징 행만 삭제 */
    @PatchMapping("/ingredients/{ingredientId}/apache-image-replace-result")
    public ResponseEntity<ApiResponse<IngredientResponse>> applyApacheImageReplaceResult(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("ingredientId") Long ingredientId,
            @RequestBody RemoteImageStagingResultRequest body) {
        long uid = requireUserId(userId);
        IngredientResponse data =
                fridgeIngredientService.applyApacheImageReplaceStagingResult(uid, ingredientId, body);
        return ResponseEntity.ok(ApiResponse.success(data, "이미지 교체 반영"));
    }

    @DeleteMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<DeleteIngredientData>> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
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

package com.today.fridge.user.controller;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.UserPhysicalMetricsRequest;
import com.today.fridge.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    // 신체 정보 추가
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addPhysicalMetrics(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserPhysicalMetricsRequest request) {
        log.info("[UserProfileController] addPhysicalMetrics - userId: {}", userId);
        userProfileService.updatePhysicalMetrics(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보가 추가되었습니다."));
    }

    // 신체 정보 수정
    @PostMapping("/alter")
    public ResponseEntity<ApiResponse<Void>> alterPhysicalMetrics(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserPhysicalMetricsRequest request) {
        log.info("[UserProfileController] alterPhysicalMetrics - userId: {}", userId);
        userProfileService.updatePhysicalMetrics(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보가 수정되었습니다."));
    }

    // 신체 정보 삭제
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deletePhysicalMetrics(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("[UserProfileController] deletePhysicalMetrics - userId: {}", userId);
        userProfileService.clearPhysicalMetrics(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보가 삭제되었습니다."));
    }
}

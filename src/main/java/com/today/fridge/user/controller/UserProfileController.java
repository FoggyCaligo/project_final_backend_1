package com.today.fridge.user.controller;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.UserPhysicalMetricsRequest;
import com.today.fridge.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<ApiResponse<Void>> addPhysicalMetrics(@Valid @RequestBody UserPhysicalMetricsRequest request) {
        String loginId = getLoginIdFromAuth();
        log.info("[UserProfileController] addPhysicalMetrics - loginId: {}", loginId);
        userProfileService.updatePhysicalMetrics(loginId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보가 추가되었습니다."));
    }

    // 신체 정보 수정
    @PostMapping("/alter")
    public ResponseEntity<ApiResponse<Void>> alterPhysicalMetrics(@Valid @RequestBody UserPhysicalMetricsRequest request) {
        String loginId = getLoginIdFromAuth();
        log.info("[UserProfileController] alterPhysicalMetrics - loginId: {}", loginId);
        userProfileService.updatePhysicalMetrics(loginId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보가 수정되었습니다."));
    }

    // 신체 정보 삭제
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deletePhysicalMetrics() {
        String loginId = getLoginIdFromAuth();
        log.info("[UserProfileController] deletePhysicalMetrics - loginId: {}", loginId);
        userProfileService.clearPhysicalMetrics(loginId);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보가 삭제되었습니다."));
    }

    private String getLoginIdFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new com.today.fridge.global.exception.ExceptionTemplate(
                    com.today.fridge.global.exception.ErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }
}

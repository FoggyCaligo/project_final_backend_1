package com.today.fridge.user.controller;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.UserPhysicalMetricsRequest;
import com.today.fridge.user.dto.response.ProfileResponse;
import com.today.fridge.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<ProfileResponse> addPhysicalMetrics(@Valid @RequestBody UserPhysicalMetricsRequest request) {
        String loginId = getLoginIdFromAuth();
        log.info("[UserProfileController] addPhysicalMetrics - loginId: {}", loginId);
        ProfileResponse profile = userProfileService.updatePhysicalMetrics(loginId, request);
        return ApiResponse.success(profile, "신체 정보가 추가되었습니다.");
    }

    // 신체 정보 수정
    @PostMapping("/alter")
    public ApiResponse<ProfileResponse> alterPhysicalMetrics(@Valid @RequestBody UserPhysicalMetricsRequest request) {
        String loginId = getLoginIdFromAuth();
        log.info("[UserProfileController] alterPhysicalMetrics - loginId: {}", loginId);
        ProfileResponse profile = userProfileService.updatePhysicalMetrics(loginId, request);
        return ApiResponse.success(profile, "신체 정보가 수정되었습니다.");
    }

    // 신체 정보 삭제
    @PostMapping("/delete")
    public ApiResponse<ProfileResponse> deletePhysicalMetrics() {
        String loginId = getLoginIdFromAuth();
        log.info("[UserProfileController] deletePhysicalMetrics - loginId: {}", loginId);
        ProfileResponse profile = userProfileService.clearPhysicalMetrics(loginId);
        return ApiResponse.success(profile, "신체 정보가 삭제되었습니다.");
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

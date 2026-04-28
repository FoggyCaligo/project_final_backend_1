package com.today.fridge.user.controller;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.PasswordChangeRequest;
import com.today.fridge.user.dto.request.ProfileUpdateRequest;
import com.today.fridge.user.dto.response.ProfileResponse;
import com.today.fridge.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 비밀번호 찾기 (email): 이메일로 가입된 아이디 반환
    @GetMapping("/find-loginid")
    public ApiResponse<String> findLoginId(
            @RequestParam @NotBlank @Email(message = "올바른 이메일 형식이 아닙니다.") String email) {
        String loginId = userService.findLoginIdByEmail(email);
        return ApiResponse.success(loginId, "아이디를 찾았습니다.");
    }

    // 팀 공식 스펙: GET /api/v1/users/me/profile — 마이페이지 조회
    @GetMapping("/me/profile")
    public ApiResponse<ProfileResponse> getProfile() {
        String loginId = getLoginIdFromAuth();
        ProfileResponse profile = userService.getProfile(loginId);
        return ApiResponse.success(profile, "프로필 정보입니다.");
    }

    // 팀 공식 스펙: PATCH /api/v1/users/me/profile — 마이페이지 수정
    @PatchMapping("/me/profile")
    public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        String loginId = getLoginIdFromAuth();
        ProfileResponse profile = userService.updateProfile(loginId, request);
        return ApiResponse.success(profile, "프로필이 수정되었습니다.");
    }

    // 팀 공식 스펙: PATCH /api/v1/users/me/password — 비밀번호 변경
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        String loginId = getLoginIdFromAuth();
        userService.changePassword(loginId, request);
        return ApiResponse.success(null, "비밀번호가 변경되었습니다.");
    }

    // SecurityContext에서 현재 인증된 사용자의 loginId 추출
    private String getLoginIdFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new com.today.fridge.global.exception.ExceptionTemplate(
                    com.today.fridge.global.exception.ErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }
}

package com.today.fridge.auth.kakao.controller;

import com.today.fridge.auth.dto.LoginResponse;
import com.today.fridge.auth.kakao.service.KakaoAuthService;
import com.today.fridge.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;

    /** 카카오 OAuth 콜백 - 인가 코드로 로그인 처리 */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<LoginResponse>> callback(
            @RequestParam String code) {
        LoginResponse response = kakaoAuthService.kakaoLogin(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 카카오 로그아웃 - 카카오 액세스 토큰 무효화 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestParam String accessToken) {
        kakaoAuthService.kakaoLogout(accessToken);
        return ResponseEntity.ok(ApiResponse.success("카카오 로그아웃이 완료되었습니다", null));
    }
}

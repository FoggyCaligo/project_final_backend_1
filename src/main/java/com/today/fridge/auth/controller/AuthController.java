package com.today.fridge.auth.controller;

import com.today.fridge.auth.dto.request.LoginRequest;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final AuthService authService;
    private final UserService userService;

    public AuthController(JwtProvider jwtProvider, AuthService authService, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = authService.authenticate(request.getLoginId(), request.getPassword());

        // 이메일 미인증 사용자 로그인 차단
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new com.today.fridge.global.exception.ExceptionTemplate(
                    com.today.fridge.global.exception.ErrorCode.EMAIL_NOT_VERIFIED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getLoginId());
        String refreshToken = jwtProvider.createRefreshToken(user.getLoginId());

        authService.createSession(user, refreshToken, 1209600000L);

        ResponseCookie accessCookie = jwtProvider.createTokenCookie("accessToken", accessToken, 3600000L);
        ResponseCookie refreshCookie = jwtProvider.createTokenCookie("refreshToken", refreshToken, 1209600000L);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ApiResponse.success(null, "로그인되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtProvider.resolveTokenFromCookie(request, "refreshToken");

        if (refreshToken != null) {
            authService.invalidateSession(refreshToken);
        }

        ResponseCookie cleanAccessCookie = jwtProvider.createTokenCookie("accessToken", "", 0L);
        ResponseCookie cleanRefreshCookie = jwtProvider.createTokenCookie("refreshToken", "", 0L);

        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString());

        return ApiResponse.success(null, "로그아웃되었습니다.");
    }

    // 팀 공식 스펙: POST /api/v1/auth/signup — 회원가입 (UserController에서 이동)
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return ApiResponse.success(null, "회원가입이 완료되었습니다.");
    }

    // 팀 공식 스펙: GET /api/v1/auth/check-login-id — 아이디 중복 확인
    @GetMapping("/check-login-id")
    public ApiResponse<Map<String, Boolean>> checkLoginId(@RequestParam String loginId) {
        boolean available = userService.isLoginIdAvailable(loginId);
        return ApiResponse.success(Map.of("available", available), available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.");
    }

    // 팀 공식 스펙: GET /api/v1/auth/me — 현재 인증된 사용자 정보 조회
    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다.");
        }
        String loginId = auth.getName();
        String nickname = userService.getProfile(loginId).getNickname();
        return ApiResponse.success(Map.of("loginId", loginId, "nickname", nickname), "현재 사용자 정보입니다.");
    }

    // 팀 공식 스펙: POST /api/v1/auth/refresh — Refresh Token 재발급
    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtProvider.resolveTokenFromCookie(request, "refreshToken");

        // 기존 세션 검증 + revoke → loginId 반환
        String loginId = authService.refreshSession(refreshToken);

        // 새 토큰 쌍 발급
        String newAccessToken = jwtProvider.createAccessToken(loginId);
        String newRefreshToken = jwtProvider.createRefreshToken(loginId);

        // 새 세션 생성
        User user = authService.authenticate(loginId);
        authService.createSession(user, newRefreshToken, 1209600000L);

        // 새 쿠키 설정
        ResponseCookie accessCookie = jwtProvider.createTokenCookie("accessToken", newAccessToken, 3600000L);
        ResponseCookie refreshCookie = jwtProvider.createTokenCookie("refreshToken", newRefreshToken, 1209600000L);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ApiResponse.success(null, "토큰이 재발급되었습니다.");
    }

    // 이메일 인증 확인: GET /api/v1/auth/verify-email?token=xxx
    @GetMapping("/verify-email")
    public org.springframework.http.ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        // 인증 완료 후 프론트엔드 로그인 페이지로 리다이렉트
        return org.springframework.http.ResponseEntity.status(302)
                .location(URI.create("http://localhost:3000?emailVerified=true"))
                .build();
    }

    // 인증 이메일 재발송: POST /api/v1/auth/resend-verification
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@RequestParam String email) {
        userService.resendVerificationEmail(email);
        return ApiResponse.success(null, "인증 이메일이 재발송되었습니다.");
    }
}
// 레디쉬를 통한 token, refreshtoken 관리, 
// reffreshtoken 30분마다 발급
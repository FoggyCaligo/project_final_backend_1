package com.today.fridge.auth.controller;

import com.today.fridge.auth.dto.request.LoginRequest;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.auth.service.KakaoOAuthService;
import com.today.fridge.auth.service.RedisEmailVerifyService;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.external.EmailService;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import com.today.fridge.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Redis 기반 인증 컨트롤러 (AuthController + KakaoAuthController 통합).
 *
 * 기존 /api/v1/auth 경로를 유지하면서 /api/v1/auth 경로로 분리하여,
 * 팀원 코드에 영향 없이 Redis 기반 토큰 관리를 적용합니다.
 *
 * 변경 사항:
 * - Access Token: 15분, Refresh Token: 7일
 * - 로그아웃 시 Access Token Redis 블랙리스트 등록
 * - Refresh Token을 Redis에 저장 (TTL 기반 자동 만료)
 * - 이메일 인증 토큰을 Redis에 저장 (24시간 TTL)
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // Access Token: 15분
    private static final long ACCESS_TOKEN_MS = 900_000L;
    // Refresh Token: 7일
    private static final long REFRESH_TOKEN_MS = 604_800_000L;

    private final JwtProvider jwtProvider;
    private final AuthService authService2;
    private final UserService userService;
    private final RedisEmailVerifyService redisEmailVerifyService;
    private final EmailService emailService;
    private final KakaoOAuthService kakaoOAuthService;
    private final UserRepository userRepository;

    @Value("${app.kakao.rest-api-key}")
    private String restApiKey;

    // 카카오 redirect URI
    @Value("${app.kakao.redirect-uri:http://localhost:8080/api/v1/auth/kakao/callback}")
    private String redirectUri;

    @Value("${app.kakao.frontend-base-url}")
    private String frontendBaseUrl;

    public AuthController(JwtProvider jwtProvider,
                           AuthService authService2,
                           UserService userService,
                           RedisEmailVerifyService redisEmailVerifyService,
                           EmailService emailService,
                           KakaoOAuthService kakaoOAuthService,
                           UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.authService2 = authService2;
        this.userService = userService;
        this.redisEmailVerifyService = redisEmailVerifyService;
        this.emailService = emailService;
        this.kakaoOAuthService = kakaoOAuthService;
        this.userRepository = userRepository;
    }

    // ──────────────────────────────────────────────
    // 일반 로그인 / 로그아웃 / 토큰 갱신
    // ──────────────────────────────────────────────

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = authService2.authenticate(request.getLoginId(), request.getPassword());

        // 이메일 미인증 사용자 로그인 차단
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ExceptionTemplate(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getLoginId());
        String refreshToken = jwtProvider.createRefreshToken(user.getLoginId());

        // Redis에 Refresh Token 세션 생성
        authService2.createSession(user, refreshToken, REFRESH_TOKEN_MS);

        setTokenCookies(response, accessToken, refreshToken);
        user.updateLastLoginAt();

        String loginType = user.getLoginId().startsWith("kakao_") ? "kakao" : "general";
        Map<String, Object> data = Map.of(
                "userId", user.getUserId(),
                "loginId", user.getLoginId(),
                "nickname", user.getNickname(),
                "loginType", loginType
        );
        return ApiResponse.success(data, "로그인되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtProvider.resolveTokenFromCookie(request, "accessToken");
        String refreshToken = jwtProvider.resolveTokenFromCookie(request, "refreshToken");

        // Access Token 블랙리스트 + Refresh Token 삭제
        long remainingMs = accessToken != null ? jwtProvider.getRemainingMs(accessToken) : 0;
        authService2.invalidateSession(refreshToken, accessToken, remainingMs);

        // 쿠키 삭제
        clearTokenCookies(response);

        return ApiResponse.success(null, "로그아웃되었습니다.");
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtProvider.resolveTokenFromCookie(request, "refreshToken");

        // 기존 세션 검증 + 로테이션 (기존 토큰 삭제 → loginId 반환)
        String loginId = authService2.refreshSession(refreshToken);

        // 새 토큰 쌍 발급
        String newAccessToken = jwtProvider.createAccessToken(loginId);
        String newRefreshToken = jwtProvider.createRefreshToken(loginId);

        // 새 Redis 세션 생성
        User user = authService2.authenticate(loginId);
        authService2.createSession(user, newRefreshToken, REFRESH_TOKEN_MS);

        setTokenCookies(response, newAccessToken, newRefreshToken);

        return ApiResponse.success(null, "토큰이 재발급되었습니다.");
    }

    // ──────────────────────────────────────────────
    // 회원가입 / 사용자 정보
    // ──────────────────────────────────────────────

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        // 기존 UserService의 회원가입 로직 사용
        userService.signup(request);

        // Redis에 이메일 인증 토큰도 추가 저장 (DB + Redis 이중 저장, 전환기)
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElse(null);
        if (user != null) {
            String redisToken = redisEmailVerifyService.createVerifyToken(user.getLoginId());
            // Redis 토큰으로 인증 이메일 발송 (DB 토큰과 별도)
            emailService.sendVerificationEmail(user.getEmail(), redisToken);
        }

        return ApiResponse.success(null, "회원가입이 완료되었습니다. 인증 이메일을 확인해주세요.");
    }

    @GetMapping("/check-login-id")
    public ApiResponse<Map<String, Boolean>> checkLoginId(@RequestParam String loginId) {
        boolean available = userService.isLoginIdAvailable(loginId);
        return ApiResponse.success(
                Map.of("available", available),
                available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.");
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다.");
        }
        var profile = userService.getProfileByUserId(userId);
        return ApiResponse.success(
                Map.of("userId", userId, "loginId", profile.getLoginId(), "nickname", profile.getNickname()),
                "현재 사용자 정보입니다.");
    }

    // ──────────────────────────────────────────────
    // 이메일 인증 (Redis 기반)
    // ──────────────────────────────────────────────

    @GetMapping("/verify-email")
    public org.springframework.http.ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        // Redis에서 토큰 검증
        String loginId = redisEmailVerifyService.verifyToken(token);

        if (loginId != null) {
            // Redis 인증 성공 → DB 상태도 업데이트
            User user = userRepository.findByLoginId(loginId).orElse(null);
            if (user != null) {
                user.verifyEmail();
                userRepository.save(user);
            }
        } else {
            // Redis에 없으면 기존 DB 토큰으로 fallback 시도
            userService.verifyEmail(token);
        }

        return org.springframework.http.ResponseEntity.status(302)
                .location(URI.create(frontendBaseUrl + "?emailVerified=true"))
                .build();
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@RequestParam String email) {
        String normalizedEmail = userService.normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ExceptionTemplate(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // Redis에 새 토큰 생성
        String newToken = redisEmailVerifyService.regenerateToken(user.getLoginId(), null);
        emailService.sendVerificationEmail(normalizedEmail, newToken);

        return ApiResponse.success(null, "인증 이메일이 재발송되었습니다.");
    }

    // ──────────────────────────────────────────────
    // 카카오 소셜 로그인
    // ──────────────────────────────────────────────

    @GetMapping("/kakao/login")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + restApiKey
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=profile_nickname,profile_image";
        response.sendRedirect(kakaoAuthUrl);
    }

    @GetMapping("/kakao/callback")
    public void kakaoCallback(@RequestParam String code,
                              @RequestParam(required = false) String error,
                              HttpServletResponse response) throws IOException {
        if (error != null) {
            log.warn("[KakaoOAuth2] 사용자가 카카오 로그인을 취소했습니다.");
            response.sendRedirect(frontendBaseUrl + "/login?error=true");
            return;
        }

        try {
            // 1. 인가 코드 → 카카오 액세스 토큰
            String kakaoAccessToken = kakaoOAuthService.getKakaoAccessToken(code);

            // 2. 카카오 프로필 조회
            KakaoOAuthService.KakaoUserProfile profile =
                    kakaoOAuthService.getKakaoUserProfile(kakaoAccessToken);

            // 3. DB에서 사용자 조회 or 생성
            User user = kakaoOAuthService.findOrCreateUser(profile);

            // 4. Redis 기반 JWT 발급
            String accessToken = jwtProvider.createAccessToken(user.getLoginId());
            String refreshToken = jwtProvider.createRefreshToken(user.getLoginId());
            authService2.createSession(user, refreshToken, REFRESH_TOKEN_MS);

            // 5. 쿠키 설정
            setTokenCookies(response, accessToken, refreshToken);

            // 6. 프론트엔드 리다이렉트
            String encodedLoginId = URLEncoder.encode(user.getLoginId(), StandardCharsets.UTF_8);
            String encodedNickname = URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendBaseUrl + "/dashboard?kakaoLogin=success&loginId=" + encodedLoginId + "&nickname=" + encodedNickname);

        } catch (Exception e) {
            log.error("[KakaoOAuth2] 로그인 처리 중 오류: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            response.sendRedirect(frontendBaseUrl + "/login?error=true");
        }
    }

    // ──────────────────────────────────────────────
    // 유틸 메서드
    // ──────────────────────────────────────────────

    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = jwtProvider.createTokenCookie("accessToken", accessToken, ACCESS_TOKEN_MS);
        ResponseCookie refreshCookie = jwtProvider.createTokenCookie("refreshToken", refreshToken, REFRESH_TOKEN_MS);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie cleanAccess = jwtProvider.createTokenCookie("accessToken", "", 0L);
        ResponseCookie cleanRefresh = jwtProvider.createTokenCookie("refreshToken", "", 0L);
        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString());
    }
}

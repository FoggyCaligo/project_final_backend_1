package com.today.fridge.auth.controller;

import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.auth.service.KakaoOAuthService;
import com.today.fridge.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// @RestController
// @RequestMapping("/api/v1/auth2/kakao")
public class KakaoAuthController {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthController.class);

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtProvider jwtProvider;
    private final AuthService authService;

    @Value("${app.kakao.rest-api-key}")
    private String restApiKey;

    @Value("${app.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${app.kakao.frontend-base-url}")
    private String frontendBaseUrl;

    public KakaoAuthController(KakaoOAuthService kakaoOAuthService,
                               JwtProvider jwtProvider,
                               AuthService authService) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.jwtProvider = jwtProvider;
        this.authService = authService;
    }

    /** 카카오 인증 페이지로 리다이렉트 */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + restApiKey
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=profile_nickname,profile_image";
        response.sendRedirect(kakaoAuthUrl);
    }

    /** 카카오 인가 코드 콜백 — JWT 발급 후 프론트엔드로 리다이렉트 */
    // AuthController와 매핑이 중복되므로 주석 처리합니다. (AuthController의 kakaoCallback 사용)
    // @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(required = false) String error,
                         HttpServletResponse response) throws IOException {

        if (error != null) {
            log.warn("[KakaoOAuth] 사용자가 카카오 로그인을 취소했습니다.");
            response.sendRedirect(frontendBaseUrl + "?kakaoError=cancelled");
            return;
        }

        try {
            // 1. 인가 코드 → 카카오 액세스 토큰
            log.info("[KakaoOAuth] 토큰 교환 시작 code={}", code.substring(0, Math.min(code.length(), 10)) + "...");
            String kakaoAccessToken = kakaoOAuthService.getKakaoAccessToken(code);
            log.info("[KakaoOAuth] 토큰 교환 성공");

            // 2. 카카오 액세스 토큰 → 사용자 프로필
            KakaoOAuthService.KakaoUserProfile profile = kakaoOAuthService.getKakaoUserProfile(kakaoAccessToken);
            log.info("[KakaoOAuth] 프로필 조회 성공 kakaoId={}", profile.kakaoId());

            // 3. users 테이블에서 조회 or 신규 생성
            User user = kakaoOAuthService.findOrCreateUser(profile);
            log.info("[KakaoOAuth] 사용자 확보 loginId={}", user.getLoginId());

            // 4. 서비스 JWT 발급
            String accessToken = jwtProvider.createAccessToken(user.getLoginId());
            String refreshToken = jwtProvider.createRefreshToken(user.getLoginId());
            authService.createSession(user, refreshToken, 1209600000L);

            // 5. HTTP-only 쿠키 설정
            ResponseCookie accessCookie = jwtProvider.createTokenCookie("accessToken", accessToken, 3600000L);
            ResponseCookie refreshCookie = jwtProvider.createTokenCookie("refreshToken", refreshToken, 1209600000L);
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // 6. 프론트엔드로 리다이렉트 (loginId 전달)
            String encodedLoginId = URLEncoder.encode(user.getLoginId(), StandardCharsets.UTF_8);
            String encodedNickname = URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendBaseUrl
                    + "?kakaoLogin=success"
                    + "&loginId=" + encodedLoginId
                    + "&nickname=" + encodedNickname);

        } catch (Exception e) {
            log.error("[KakaoOAuth] 로그인 처리 중 오류: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            response.sendRedirect(frontendBaseUrl + "?kakaoError=failed");
        }
    }
}

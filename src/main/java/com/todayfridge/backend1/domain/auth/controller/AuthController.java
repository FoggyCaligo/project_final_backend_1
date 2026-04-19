package com.todayfridge.backend1.domain.auth.controller;

import com.todayfridge.backend1.domain.auth.service.AuthService;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.auth.CookieTokenService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.CsrfCookieService;
import com.todayfridge.backend1.global.security.LoginUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {
    private final AuthService authService;
    private final CookieTokenService cookieTokenService;
    private final CsrfCookieService csrfCookieService;

    @Value("${app.security.jwt.access-token-validity-seconds}")
    private long accessValidity;
    @Value("${app.security.jwt.refresh-token-validity-seconds}")
    private long refreshValidity;

    public AuthController(AuthService authService, CookieTokenService cookieTokenService, CsrfCookieService csrfCookieService) {
        this.authService = authService;
        this.cookieTokenService = cookieTokenService;
        this.csrfCookieService = csrfCookieService;
    }

    @GetMapping("/csrf-token")
    public ApiResponse<Map<String, String>> csrfToken(CsrfToken csrfToken) {
        return ApiResponse.success(csrfCookieService.expose(csrfToken));
    }

    @PostMapping("/signup")
    public ApiResponse<Map<String, Object>> signup(@RequestBody SignupRequest request) {
        User user = authService.signup(request.email(), request.password(), request.nickname());
        return ApiResponse.success("회원가입이 완료되었습니다.", Map.of(
                "userId", user.getId(), "email", user.getEmail(), "nickname", user.getNickname()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        User user = authService.authenticate(request.email(), request.password());
        Map<String, Object> issued = authService.issueTokens(user);
        HttpHeaders headers = new HttpHeaders();
        cookieTokenService.addAccessTokenCookie(headers, (String) issued.get("accessToken"), accessValidity);
        cookieTokenService.addRefreshTokenCookie(headers, (String) issued.get("refreshToken"), refreshValidity);
        issued.remove("accessToken");
        issued.remove("refreshToken");
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success("로그인에 성공했습니다.", issued));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(HttpServletRequest request) {
        Map<String, Object> issued = authService.reissue(cookieValue(request, "refresh_token"));
        HttpHeaders headers = new HttpHeaders();
        cookieTokenService.addAccessTokenCookie(headers, (String) issued.get("accessToken"), accessValidity);
        cookieTokenService.addRefreshTokenCookie(headers, (String) issued.get("refreshToken"), refreshValidity);
        issued.remove("accessToken");
        issued.remove("refreshToken");
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success("토큰을 재발급했습니다.", issued));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(cookieValue(request, "refresh_token"));
        HttpHeaders headers = new HttpHeaders();
        cookieTokenService.clearAuthCookies(headers);
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success("로그아웃 되었습니다.", null));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@CurrentUser LoginUser loginUser) {
        User user = authService.getMe(loginUser.getUserId());
        return ApiResponse.success(Map.of(
                "userId", user.getId(), "email", user.getEmail(), "nickname", user.getNickname(), "role", user.getRole()
        ));
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public record SignupRequest(@Email @NotBlank String email, @NotBlank String password, @NotBlank String nickname) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
}

package com.today.fridge.auth.controller;

import com.today.fridge.auth.dto.request.LoginRequest;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final AuthService authService;

    public AuthController(JwtProvider jwtProvider, AuthService authService) {
        this.jwtProvider = jwtProvider;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = authService.authenticate(request.getLoginId(), request.getPassword());

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
}
// 레디쉬를 통한 token, refreshtoken 관리, 
// reffreshtoken 30분마다 발급
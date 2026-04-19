package com.todayfridge.backend1.global.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieTokenService {
    @Value("${app.security.cookie.secure:false}")
    private boolean secure;
    @Value("${app.security.cookie.same-site:Lax}")
    private String sameSite;

    public void addAccessTokenCookie(HttpHeaders headers, String token, long maxAgeSeconds) {
        headers.add(HttpHeaders.SET_COOKIE, build("access_token", token, maxAgeSeconds).toString());
    }

    public void addRefreshTokenCookie(HttpHeaders headers, String token, long maxAgeSeconds) {
        headers.add(HttpHeaders.SET_COOKIE, build("refresh_token", token, maxAgeSeconds).toString());
    }

    public void clearAuthCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, build("access_token", "", 0).toString());
        headers.add(HttpHeaders.SET_COOKIE, build("refresh_token", "", 0).toString());
    }

    private ResponseCookie build(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}

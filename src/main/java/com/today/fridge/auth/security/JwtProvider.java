package com.today.fridge.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public JwtProvider(
            @Value("${app.security.jwt.secret:VG9kYXlGcmlkZ2VQcm9qZWN0QmFja2VuZDFTdGFydGVyU2VjcmV0S2V5Rm9ySldU}") String secretKey,
            @Value("${app.security.jwt.access-token-validity-seconds:3600}") long accessTokenValiditySeconds,
            @Value("${app.security.jwt.refresh-token-validity-seconds:1209600}") long refreshTokenValiditySeconds,
            @Value("${app.security.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.security.cookie.same-site:Lax}")String cookieSameSite) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenValidity = accessTokenValiditySeconds * 1000;
        this.refreshTokenValidity = refreshTokenValiditySeconds * 1000;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    public String createAccessToken(String loginId) {
        return createToken(loginId, accessTokenValidity);
    }

    public String createRefreshToken(String loginId) {
        return createToken(loginId, refreshTokenValidity);
    }

    private String createToken(String loginId, long validity) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);
        return Jwts.builder()
                .subject(loginId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public ResponseCookie createTokenCookie(String cookieName, String token, long maxAgeMs) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAgeMs / 1000)
                .build();
    }

    public String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getLoginIdFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    /**
     * 토큰의 남은 만료 시간(밀리초)을 반환합니다.
     * 이미 만료된 토큰이면 0을 반환합니다.
     */
    public long getRemainingMs(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            long expirationMs = claims.getExpiration().getTime();
            long remaining = expirationMs - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }
}

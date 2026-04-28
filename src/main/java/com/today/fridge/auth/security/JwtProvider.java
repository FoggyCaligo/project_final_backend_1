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

    public JwtProvider(
            @Value("${jwt.secret:defaultSecretKeyWithAtLeast32CharactersForHs256!!}") String secretKey,
            @Value("${jwt.access-token-validity-in-ms:3600000}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity-in-ms:1209600000}") long refreshTokenValidity) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
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
                .secure(true)
                .sameSite("Lax")
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
}

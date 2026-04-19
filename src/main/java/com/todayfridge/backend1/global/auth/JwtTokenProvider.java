package com.todayfridge.backend1.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.access-token-validity-seconds}")
    private long accessValidity;

    @Value("${app.security.jwt.refresh-token-validity-seconds}")
    private long refreshValidity;

    public String createAccessToken(Long userId, String email, String role, String nickname) {
        return createToken(userId, email, role, nickname, accessValidity);
    }

    public String createRefreshToken(Long userId, String email, String role, String nickname) {
        return createToken(userId, email, role, nickname, refreshValidity);
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String createToken(Long userId, String email, String role, String nickname, long validity) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("nickname", nickname)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validity)))
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}

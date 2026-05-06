package com.today.fridge.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 JWT 토큰 관리 서비스.
 *
 * - Refresh Token: Redis에 hash → loginId 매핑 저장 (TTL 7일)
 * - Access Token 블랙리스트: 로그아웃 시 남은 TTL만큼 등록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    // ── Refresh Token 관리 ──

    /**
     * Refresh Token 해시를 Redis에 저장합니다.
     */
    public void saveRefreshToken(String tokenHash, String loginId, long ttlMs) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + tokenHash, loginId, ttlMs, TimeUnit.MILLISECONDS);
        log.debug("[RedisTokenService] Refresh token 저장 완료 loginId={}", loginId);
    }

    /**
     * Refresh Token 해시로 loginId를 조회합니다.
     * @return loginId 또는 null (만료/미존재)
     */
    public String getLoginIdByRefreshToken(String tokenHash) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + tokenHash);
    }

    /**
     * Refresh Token을 삭제합니다 (로그아웃 또는 로테이션 시).
     */
    public void deleteRefreshToken(String tokenHash) {
        redisTemplate.delete(REFRESH_PREFIX + tokenHash);
    }

    // ── Access Token 블랙리스트 ──

    /**
     * 로그아웃된 Access Token을 블랙리스트에 등록합니다.
     * TTL은 토큰의 남은 만료 시간으로 설정하여 자연 소멸합니다.
     */
    public void blacklistAccessToken(String accessToken, long remainingMs) {
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken, "logout", remainingMs, TimeUnit.MILLISECONDS);
            log.debug("[RedisTokenService] Access token 블랙리스트 등록 (TTL={}ms)", remainingMs);
        }
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인합니다.
     */
    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
    }

    // ── 유틸 ──

    /**
     * 토큰 문자열을 SHA-256으로 해싱합니다.
     */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("토큰 해싱 중 오류가 발생했습니다.", e);
        }
    }
}

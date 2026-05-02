package com.today.fridge.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 이메일 인증 토큰 관리 서비스.
 *
 * - 토큰 생성 시 Redis에 저장 (TTL 24시간 → 자동 만료)
 * - 인증 완료 시 토큰 즉시 삭제 (일회용)
 * - DB의 email_verify_token 컬럼 대체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisEmailVerifyService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "email:verify:";
    private static final long TTL_HOURS = 24;

    /**
     * 이메일 인증 토큰을 생성하고 Redis에 저장합니다.
     *
     * @param loginId 사용자의 loginId
     * @return 생성된 UUID 토큰
     */
    public String createVerifyToken(String loginId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                PREFIX + token, loginId, TTL_HOURS, TimeUnit.HOURS);
        log.info("[RedisEmailVerify] 인증 토큰 생성 loginId={}, TTL={}h", loginId, TTL_HOURS);
        return token;
    }

    /**
     * 토큰으로 loginId를 조회합니다.
     * 조회 성공 시 토큰을 즉시 삭제합니다 (일회용).
     *
     * @return loginId 또는 null (만료/미존재)
     */
    public String verifyToken(String token) {
        String key = PREFIX + token;
        String loginId = redisTemplate.opsForValue().get(key);
        if (loginId != null) {
            redisTemplate.delete(key); // 일회용: 인증 완료 후 즉시 삭제
            log.info("[RedisEmailVerify] 인증 토큰 검증 성공 loginId={}", loginId);
        }
        return loginId;
    }

    /**
     * 기존 토큰을 삭제하고 새 토큰을 재생성합니다.
     *
     * @param loginId 사용자의 loginId
     * @param oldToken 기존 토큰 (null이면 삭제 생략)
     * @return 새로 생성된 UUID 토큰
     */
    public String regenerateToken(String loginId, String oldToken) {
        if (oldToken != null) {
            redisTemplate.delete(PREFIX + oldToken);
        }
        return createVerifyToken(loginId);
    }
}

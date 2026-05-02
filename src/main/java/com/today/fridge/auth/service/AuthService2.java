package com.today.fridge.auth.service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 인증 서비스 (AuthService의 Redis 적용 버전).
 *
 * - Refresh Token 세션을 PostgreSQL user_session 대신 Redis에 저장
 * - 토큰 로테이션(사용 시 기존 삭제 + 새 발급) 지원
 */
@Slf4j
@Service
public class AuthService2 {

    private final RedisTokenService redisTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService2(RedisTokenService redisTokenService,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.redisTokenService = redisTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 로그인 인증: loginId + 비밀번호 검증
     */
    public User authenticate(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ExceptionTemplate(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * loginId만으로 사용자 조회 (refresh 토큰 재발급 시 사용)
     */
    public User authenticate(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Redis에 Refresh Token 세션 생성
     */
    public void createSession(User user, String refreshToken, long validityMs) {
        String tokenHash = RedisTokenService.hashToken(refreshToken);
        redisTokenService.saveRefreshToken(tokenHash, user.getLoginId(), validityMs);
        log.info("[AuthService2] Redis 세션 생성 완료 loginId={}", user.getLoginId());
    }

    /**
     * 로그아웃: Refresh Token 세션 삭제 + Access Token 블랙리스트 등록
     */
    public void invalidateSession(String refreshToken, String accessToken, long accessTokenRemainingMs) {
        if (refreshToken != null) {
            String tokenHash = RedisTokenService.hashToken(refreshToken);
            redisTokenService.deleteRefreshToken(tokenHash);
        }
        if (accessToken != null) {
            redisTokenService.blacklistAccessToken(accessToken, accessTokenRemainingMs);
        }
        log.info("[AuthService2] 세션 무효화 완료");
    }

    /**
     * Refresh Token 검증 + 세션 유효성 확인 → loginId 반환 (로테이션: 기존 토큰 삭제)
     */
    public String refreshSession(String refreshToken) {
        if (refreshToken == null) {
            throw new ExceptionTemplate(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        String tokenHash = RedisTokenService.hashToken(refreshToken);
        String loginId = redisTokenService.getLoginIdByRefreshToken(tokenHash);

        if (loginId == null) {
            throw new ExceptionTemplate(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 기존 토큰 삭제 (로테이션)
        redisTokenService.deleteRefreshToken(tokenHash);
        log.info("[AuthService2] Refresh 로테이션 완료 loginId={}", loginId);
        return loginId;
    }
}

package com.today.fridge.auth.service;

import com.today.fridge.auth.entity.UserSession;
import com.today.fridge.auth.repository.UserSessionRepository;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserSessionRepository userSessionRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ExceptionTemplate(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    // loginId만으로 사용자 조회 (refresh 토큰 재발급 시 사용)
    public User authenticate(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void createSession(User user, String refreshToken, long validityInMs) {
        String tokenHash = hashToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(validityInMs * 1_000_000);
        UserSession session = UserSession.create(user, tokenHash, expiresAt);
        userSessionRepository.save(session);
    }

    @Transactional
    public void invalidateSession(String refreshToken) {
        if (refreshToken == null) return;
        String tokenHash = hashToken(refreshToken);
        userSessionRepository.findByRefreshTokenHash(tokenHash)
                .ifPresent(UserSession::revoke);
    }

    // Refresh Token 검증 + 세션 유효성 확인 → loginId 반환
    @Transactional
    public String refreshSession(String refreshToken) {
        if (refreshToken == null) {
            throw new ExceptionTemplate(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        String tokenHash = hashToken(refreshToken);
        UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.REFRESH_TOKEN_EXPIRED));

        if (!session.isValid()) {
            throw new ExceptionTemplate(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 기존 세션 revoke (토큰 로테이션)
        session.revoke();
        return session.getUser().getLoginId();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("토큰 해싱 중 오류가 발생했습니다.", e);
        }
    }
}

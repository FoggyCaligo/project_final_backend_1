package com.todayfridge.backend1.domain.auth.service;

import com.todayfridge.backend1.domain.auth.entity.RefreshToken;
import com.todayfridge.backend1.domain.auth.repository.RefreshTokenRepository;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import com.todayfridge.backend1.global.util.FileUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void save(User user, String refreshToken, long validitySeconds) {
        refreshTokenRepository.save(new RefreshToken(user, FileUtils.sha256(refreshToken), LocalDateTime.now().plusSeconds(validitySeconds)));
    }

    @Transactional(readOnly = true)
    public RefreshToken getValidToken(String refreshToken) {
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(FileUtils.sha256(refreshToken))
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "유효한 리프레시 토큰이 없습니다."));
    }

    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHashAndRevokedFalse(FileUtils.sha256(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }
}

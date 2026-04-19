package com.todayfridge.backend1.domain.user.service;

import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.domain.user.repository.UserRepository;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserCommandService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCommandService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> updateProfile(Long userId, String nickname) {
        User user = getUser(userId);
        user.changeProfile(nickname);
        return Map.of("userId", user.getId(), "nickname", user.getNickname());
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}

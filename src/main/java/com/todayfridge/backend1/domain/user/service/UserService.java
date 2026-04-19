package com.todayfridge.backend1.domain.user.service;

import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.domain.user.repository.UserRepository;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> profile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "role", user.getRole(),
                "status", user.getStatus()
        );
    }
}

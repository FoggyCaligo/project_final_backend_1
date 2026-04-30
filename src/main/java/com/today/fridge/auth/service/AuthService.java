package com.today.fridge.auth.service;

import com.today.fridge.auth.dto.LoginRequest;
import com.today.fridge.auth.dto.LoginResponse;
import com.today.fridge.auth.dto.SignupRequest;
import com.today.fridge.global.security.JwtProvider;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public boolean isDuplicateLoginId(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .loginId(request.getLoginId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .status("active")
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다");
        }

        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        return new LoginResponse(user.getUserId(), user.getNickname(), accessToken);
    }
}

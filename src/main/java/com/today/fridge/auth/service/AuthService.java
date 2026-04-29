package com.today.fridge.auth.service;

import com.today.fridge.auth.dto.LoginRequest;
import com.today.fridge.auth.dto.LoginResponse;
import com.today.fridge.auth.dto.SignupRequest;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
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
            throw new ExceptionTemplate(ErrorCode.DUPLICATE_LOGIN_ID);
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
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ExceptionTemplate(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        return new LoginResponse(user.getUserId(), user.getNickname(), accessToken);
    }
}

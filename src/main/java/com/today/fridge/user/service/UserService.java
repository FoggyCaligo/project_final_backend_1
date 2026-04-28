package com.today.fridge.user.service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validatePassword(request.getPassword());

        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new ExceptionTemplate(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ExceptionTemplate(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new ExceptionTemplate(ErrorCode.DUPLICATE_NICKNAME);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.create(request.getLoginId(), normalizedEmail, passwordHash, request.getNickname());
        userRepository.save(user);
    }

    // 이메일 정규화: 공백 제거 + 소문자 변환
    public String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    // 비밀번호 정규화: 영문+숫자+특수문자 각 1자 이상, 8~30자
    public void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 30) {
            throw new ExceptionTemplate(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!hasLetter || !hasDigit || !hasSpecial) {
            throw new ExceptionTemplate(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    // 비밀번호 찾기 (email): 이메일로 아이디 반환
    @Transactional(readOnly = true)
    public String findLoginIdByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        return user.getLoginId();
    }
}

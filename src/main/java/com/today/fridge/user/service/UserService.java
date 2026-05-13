package com.today.fridge.user.service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.external.EmailService;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.user.dto.request.PasswordChangeRequest;
import com.today.fridge.user.dto.request.ProfileUpdateRequest;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.dto.response.ProfileResponse;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserConditionRepository userConditionRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            UserConditionRepository userConditionRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userConditionRepository = userConditionRepository;
    }

    // 회원가입 — 이메일 인증 토큰 생성 후 인증 메일 발송
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

        // 비동기로 인증 이메일 발송
        emailService.sendVerificationEmail(normalizedEmail, user.getEmailVerifyToken());
    }

    // 이메일 인증 처리
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new ExceptionTemplate(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        if (!user.isEmailVerifyTokenValid()) {
            throw new ExceptionTemplate(ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED);
        }

        user.verifyEmail();
    }

    // 인증 이메일 재발송
    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new ExceptionTemplate(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        user.regenerateEmailVerifyToken();
        emailService.sendVerificationEmail(normalizedEmail, user.getEmailVerifyToken());
    }

    // 이메일 정규화: 공백 제거 + 소문자 변환
    public String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    // 비밀번호 검증: 영문+숫자+특수문자 각 1자 이상, 8~30자
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

    // 아이디 중복 확인: 사용 가능하면 true, 이미 존재하면 false
    @Transactional(readOnly = true)
    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    // 마이페이지 조회: loginId로 사용자 프로필 반환
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        return ProfileResponse.from(user, getActiveConditionCodes(user.getUserId()));
    }

    // 마이페이지 조회: userId(PK)로 사용자 프로필 반환
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        return ProfileResponse.from(user, getActiveConditionCodes(user.getUserId()));
    }

    // 마이페이지 수정: 닉네임, 프로필 이미지 URL 변경
    @Transactional
    public ProfileResponse updateProfile(String loginId, ProfileUpdateRequest request) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        // 닉네임 변경 시 중복 체크 (현재 자기 자신의 닉네임은 제외)
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new ExceptionTemplate(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        user.updateProfile(request.getNickname(), request.getProfileImageUrl());
        if (request.getHeightCm() != null) {
            user.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            user.setWeightKg(request.getWeightKg());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        return ProfileResponse.from(user, getActiveConditionCodes(user.getUserId()));
    }

    private List<String> getActiveConditionCodes(Long userId) {
        return userConditionRepository.findByUser_UserIdAndIsActiveTrue(userId).stream()
                .map(userCondition -> userCondition.getConditionCode().getConditionCode())
                .toList();
    }

    // 비밀번호 변경: 현재 비밀번호 확인 후 새 비밀번호로 변경
    @Transactional
    public void changePassword(String loginId, PasswordChangeRequest request) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ExceptionTemplate(ErrorCode.UNAUTHORIZED);
        }

        validatePassword(request.getNewPassword());
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(newPasswordHash);
    }
}

package com.today.fridge.user.service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.user.dto.request.PasswordChangeRequest;
import com.today.fridge.user.dto.request.ProfileUpdateRequest;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private SignupRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new SignupRequest();
        validRequest.setLoginId("testuser1");
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("Test1234!");
        validRequest.setNickname("테스터");
    }

    // ===== signup =====

    @Test
    @DisplayName("정상적인 요청으로 회원가입에 성공한다")
    void signup_success() {
        // given
        given(userRepository.existsByLoginId(anyString())).willReturn(false);
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // when & then
        assertThatNoException().isThrownBy(() -> userService.signup(validRequest));
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("loginId 중복 시 DUPLICATE_LOGIN_ID 예외가 발생한다")
    void signup_duplicateLoginId() {
        // given
        given(userRepository.existsByLoginId("testuser1")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID));
    }

    @Test
    @DisplayName("email 중복 시 DUPLICATE_EMAIL 예외가 발생한다")
    void signup_duplicateEmail() {
        // given
        given(userRepository.existsByLoginId(anyString())).willReturn(false);
        given(userRepository.existsByEmail("test@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("nickname 중복 시 DUPLICATE_NICKNAME 예외가 발생한다")
    void signup_duplicateNickname() {
        // given
        given(userRepository.existsByLoginId(anyString())).willReturn(false);
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname("테스터")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 INVALID_INPUT_VALUE 예외가 발생한다")
    void signup_passwordTooShort() {
        // given
        validRequest.setPassword("T1!");

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("비밀번호에 특수문자가 없으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void signup_passwordNoSpecialChar() {
        // given
        validRequest.setPassword("Test12345");

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void signup_passwordNoDigit() {
        // given
        validRequest.setPassword("TestTest!");

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("비밀번호에 영문자가 없으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void signup_passwordNoLetter() {
        // given
        validRequest.setPassword("12345678!");

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    // ===== normalizeEmail =====

    @Test
    @DisplayName("이메일 정규화: 앞뒤 공백 제거 및 소문자 변환")
    void normalizeEmail_trimAndLowercase() {
        // given
        String email = "  Test@EXAMPLE.COM  ";

        // when
        String normalized = userService.normalizeEmail(email);

        // then
        assertThat(normalized).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("이메일 정규화: null 입력 시 null을 반환한다")
    void normalizeEmail_null() {
        assertThat(userService.normalizeEmail(null)).isNull();
    }

    // ===== findLoginIdByEmail =====

    @Test
    @DisplayName("등록된 이메일로 아이디를 정상 조회한다")
    void findLoginIdByEmail_success() {
        // given
        User user = User.create("testuser1", "test@example.com", "hash", "테스터");
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        // when
        String loginId = userService.findLoginIdByEmail("test@example.com");

        // then
        assertThat(loginId).isEqualTo("testuser1");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 USER_NOT_FOUND 예외가 발생한다")
    void findLoginIdByEmail_notFound() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findLoginIdByEmail("nobody@example.com"))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("findLoginIdByEmail: 대소문자 무관하게 정규화 후 조회한다")
    void findLoginIdByEmail_caseInsensitive() {
        // given
        User user = User.create("testuser1", "test@example.com", "hash", "테스터");
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        // when
        String loginId = userService.findLoginIdByEmail("TEST@EXAMPLE.COM");

        // then
        assertThat(loginId).isEqualTo("testuser1");
        then(userRepository).should().findByEmail("test@example.com");
    }

    // ===== isLoginIdAvailable =====

    @Test
    @DisplayName("사용 가능한 아이디를 확인하면 true를 반환한다")
    void isLoginIdAvailable_available() {
        // given
        given(userRepository.existsByLoginId("newuser")).willReturn(false);

        // when
        boolean result = userService.isLoginIdAvailable("newuser");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 아이디를 확인하면 false를 반환한다")
    void isLoginIdAvailable_duplicate() {
        // given
        given(userRepository.existsByLoginId("existing")).willReturn(true);

        // when
        boolean result = userService.isLoginIdAvailable("existing");

        // then
        assertThat(result).isFalse();
    }

    // ===== getProfile =====

    @Test
    @DisplayName("loginId로 프로필을 정상 조회한다")
    void getProfile_success() {
        // given
        User user = User.create("testuser1", "test@example.com", "hash", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));

        // when
        var profile = userService.getProfile("testuser1");

        // then
        assertThat(profile.getLoginId()).isEqualTo("testuser1");
        assertThat(profile.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 loginId 프로필 조회 시 USER_NOT_FOUND 예외가 발생한다")
    void getProfile_notFound() {
        // given
        given(userRepository.findByLoginId("nobody")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getProfile("nobody"))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    // ===== updateProfile =====

    @Test
    @DisplayName("닉네임 변경 시 프로필이 정상 수정된다")
    void updateProfile_success() {
        // given
        User user = User.create("testuser1", "test@example.com", "hash", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("새닉네임");

        // when
        var profile = userService.updateProfile("testuser1", req);

        // then
        assertThat(profile.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("닉네임 중복 시 DUPLICATE_NICKNAME 예외가 발생한다")
    void updateProfile_duplicateNickname() {
        // given
        User user = User.create("testuser1", "test@example.com", "hash", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉")).willReturn(true);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("중복닉");

        // when & then
        assertThatThrownBy(() -> userService.updateProfile("testuser1", req))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
    }

    @Test
    @DisplayName("자기 자신의 닉네임으로 수정 요청 시 중복 예외가 발생하지 않는다")
    void updateProfile_sameNickname_noError() {
        // given
        User user = User.create("testuser1", "test@example.com", "hash", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("테스터"); // 현재 닉네임과 동일

        // when & then
        assertThatNoException().isThrownBy(() -> userService.updateProfile("testuser1", req));
    }

    // ===== changePassword =====

    @Test
    @DisplayName("현재 비밀번호 일치 시 비밀번호를 정상 변경한다")
    void changePassword_success() {
        // given
        User user = User.create("testuser1", "test@example.com", "encodedOld", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("OldPass1!", "encodedOld")).willReturn(true);
        given(passwordEncoder.encode("NewPass1!")).willReturn("encodedNew");

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("OldPass1!");
        req.setNewPassword("NewPass1!");

        // when & then
        assertThatNoException().isThrownBy(() -> userService.changePassword("testuser1", req));
    }

    @Test
    @DisplayName("현재 비밀번호 불일치 시 UNAUTHORIZED 예외가 발생한다")
    void changePassword_wrongCurrentPassword() {
        // given
        User user = User.create("testuser1", "test@example.com", "encodedOld", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("WrongPass1!", "encodedOld")).willReturn(false);

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("WrongPass1!");
        req.setNewPassword("NewPass1!");

        // when & then
        assertThatThrownBy(() -> userService.changePassword("testuser1", req))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("새 비밀번호가 검증 규칙에 맞지 않으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void changePassword_invalidNewPassword() {
        // given
        User user = User.create("testuser1", "test@example.com", "encodedOld", "테스터");
        given(userRepository.findByLoginId("testuser1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("OldPass1!", "encodedOld")).willReturn(true);

        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("OldPass1!");
        req.setNewPassword("short"); // 너무 짧은 비밀번호

        // when & then
        assertThatThrownBy(() -> userService.changePassword("testuser1", req))
                .isInstanceOf(ExceptionTemplate.class)
                .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }
}


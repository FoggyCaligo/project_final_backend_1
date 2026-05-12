package com.today.fridge.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.auth.service.KakaoOAuthService;
import com.today.fridge.auth.service.RedisEmailVerifyService;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.exception.GlobalExceptionHandler;
import com.today.fridge.global.external.EmailService;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import com.today.fridge.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class })
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private JwtProvider jwtProvider;

        @MockBean
        private AuthService authService;

        @MockBean
        private UserService userService;

        @MockBean
        private RedisEmailVerifyService redisEmailVerifyService;

        @MockBean
        private EmailService emailService;

        @MockBean
        private KakaoOAuthService kakaoOAuthService;

        @MockBean
        private UserRepository userRepository;

        // ===== POST /api/v1/auth/signup (회원가입 — UserController에서 이동) =====

        @Test
        @DisplayName("POST /auth/signup: 정상 요청 시 success:true 응답을 반환한다")
        void signup_validRequest_returnsSuccess() throws Exception {
                // given
                Map<String, String> body = validSignupBody();
                willDoNothing().given(userService).signup(any(SignupRequest.class));
                given(userRepository.findByLoginId(anyString())).willReturn(Optional.empty());
                given(redisEmailVerifyService.createVerifyToken(anyString())).willReturn("token-uuid");
                willDoNothing().given(emailService).sendVerificationEmail(anyString(), anyString());

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다. 인증 이메일을 확인해주세요."));
        }

        @Test
        @DisplayName("POST /auth/signup: loginId 공백 요청 시 success:false 응답을 반환한다")
        void signup_blankLoginId_returnsError() throws Exception {
                // given
                Map<String, String> body = validSignupBody();
                body.put("loginId", "");

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("POST /auth/signup: loginId 중복 시 DUPLICATE_LOGIN_ID 에러 코드를 반환한다")
        void signup_duplicateLoginId_returnsErrorCode() throws Exception {
                // given
                Map<String, String> body = validSignupBody();
                willThrow(new ExceptionTemplate(ErrorCode.DUPLICATE_LOGIN_ID))
                                .given(userService).signup(any(SignupRequest.class));

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
        }

        @Test
        @DisplayName("POST /auth/signup: email 중복 시 DUPLICATE_EMAIL 에러 코드를 반환한다")
        void signup_duplicateEmail_returnsErrorCode() throws Exception {
                // given
                Map<String, String> body = validSignupBody();
                willThrow(new ExceptionTemplate(ErrorCode.DUPLICATE_EMAIL))
                                .given(userService).signup(any(SignupRequest.class));

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
        }

        // ===== GET /api/v1/auth/check-login-id =====

        @Test
        @DisplayName("GET /auth/check-login-id: 사용 가능한 아이디 확인 시 available:true를 반환한다")
        void checkLoginId_available_returnsTrue() throws Exception {
                // given
                given(userService.isLoginIdAvailable("newuser")).willReturn(true);

                // when & then
                mockMvc.perform(get("/api/v1/auth/check-login-id")
                                .param("loginId", "newuser"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.available").value(true))
                                .andExpect(jsonPath("$.message").value("사용 가능한 아이디입니다."));
        }

        @Test
        @DisplayName("GET /auth/check-login-id: 이미 사용 중인 아이디 확인 시 available:false를 반환한다")
        void checkLoginId_duplicate_returnsFalse() throws Exception {
                // given
                given(userService.isLoginIdAvailable("existing")).willReturn(false);

                // when & then
                mockMvc.perform(get("/api/v1/auth/check-login-id")
                                .param("loginId", "existing"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.available").value(false))
                                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
        }

        // ===== POST /api/v1/auth/login =====

        @Test
        @DisplayName("POST /auth/login: 정상 로그인 시 success:true와 쿠키를 반환한다")
        void login_validCredentials_returnsSuccessWithCookies() throws Exception {
                // given
                User user = User.create("testuser1", "test@example.com", "hashed", "테스터");
                user.verifyEmail();
                given(authService.authenticate("testuser1", "Test1234!")).willReturn(user);
                given(jwtProvider.createAccessToken("testuser1")).willReturn("access-token");
                given(jwtProvider.createRefreshToken("testuser1")).willReturn("refresh-token");
                given(jwtProvider.createTokenCookie(eq("accessToken"), anyString(), anyLong()))
                        .willReturn(ResponseCookie.from("accessToken", "access-token").maxAge(Duration.ofSeconds(3600)).build());
                given(jwtProvider.createTokenCookie(eq("refreshToken"), anyString(), anyLong()))
                        .willReturn(ResponseCookie.from("refreshToken", "refresh-token").maxAge(Duration.ofDays(7)).build());
                given(jwtProvider.getAccessTokenValidity()).willReturn(3_600_000L);
                willDoNothing().given(authService).createSession(any(User.class), anyString(), anyLong());

                Map<String, String> body = Map.of("loginId", "testuser1", "password", "Test1234!");

                // when & then
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("로그인되었습니다."));
        }

        @Test
        @DisplayName("POST /auth/login: 이메일 미인증 유저는 EMAIL_NOT_VERIFIED 에러를 반환한다")
        void login_emailNotVerified_returnsError() throws Exception {
                // given — emailVerified = false (기본값)
                User user = User.create("testuser1", "test@example.com", "hashed", "테스터");
                given(authService.authenticate("testuser1", "Test1234!")).willReturn(user);

                Map<String, String> body = Map.of("loginId", "testuser1", "password", "Test1234!");

                // when & then
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        }

        @Test
        @DisplayName("POST /auth/login: 잘못된 비밀번호 시 UNAUTHORIZED 에러를 반환한다")
        void login_wrongPassword_returnsUnauthorized() throws Exception {
                // given
                willThrow(new ExceptionTemplate(ErrorCode.UNAUTHORIZED))
                        .given(authService).authenticate("testuser1", "wrong");

                Map<String, String> body = Map.of("loginId", "testuser1", "password", "wrong");

                // when & then
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        // ===== POST /api/v1/auth/logout =====

        @Test
        @DisplayName("POST /auth/logout: 정상 로그아웃 시 success:true를 반환한다")
        void logout_validSession_returnsSuccess() throws Exception {
                // given
                given(jwtProvider.resolveTokenFromCookie(any(), eq("accessToken"))).willReturn("access-token");
                given(jwtProvider.resolveTokenFromCookie(any(), eq("refreshToken"))).willReturn("refresh-token");
                given(jwtProvider.getRemainingMs("access-token")).willReturn(900_000L);
                willDoNothing().given(authService).invalidateSession(anyString(), anyString(), anyLong());
                given(jwtProvider.createTokenCookie(eq("accessToken"), eq(""), eq(0L)))
                        .willReturn(ResponseCookie.from("accessToken", "").maxAge(0).build());
                given(jwtProvider.createTokenCookie(eq("refreshToken"), eq(""), eq(0L)))
                        .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).build());

                // when & then
                mockMvc.perform(post("/api/v1/auth/logout"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
        }

        // ===== POST /api/v1/auth/refresh =====

        @Test
        @DisplayName("POST /auth/refresh: 유효한 refreshToken으로 새 토큰 쌍을 발급한다")
        void refresh_validToken_returnsNewTokens() throws Exception {
                // given
                given(jwtProvider.resolveTokenFromCookie(any(), eq("refreshToken"))).willReturn("refresh-token");
                given(authService.refreshSession("refresh-token")).willReturn("testuser1");
                User user = User.create("testuser1", "test@example.com", "hashed", "테스터");
                given(authService.authenticate("testuser1")).willReturn(user);
                given(jwtProvider.createAccessToken("testuser1")).willReturn("new-access-token");
                given(jwtProvider.createRefreshToken("testuser1")).willReturn("new-refresh-token");
                given(jwtProvider.createTokenCookie(eq("accessToken"), anyString(), anyLong()))
                        .willReturn(ResponseCookie.from("accessToken", "new-access-token").maxAge(Duration.ofSeconds(3600)).build());
                given(jwtProvider.createTokenCookie(eq("refreshToken"), anyString(), anyLong()))
                        .willReturn(ResponseCookie.from("refreshToken", "new-refresh-token").maxAge(Duration.ofDays(7)).build());
                given(jwtProvider.getAccessTokenValidity()).willReturn(3_600_000L);
                willDoNothing().given(authService).createSession(any(User.class), anyString(), anyLong());

                // when & then
                mockMvc.perform(post("/api/v1/auth/refresh"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."));
        }

        @Test
        @DisplayName("POST /auth/refresh: 만료된 refreshToken 시 REFRESH_TOKEN_EXPIRED 에러를 반환한다")
        void refresh_expiredToken_returnsError() throws Exception {
                // given
                given(jwtProvider.resolveTokenFromCookie(any(), eq("refreshToken"))).willReturn("expired-token");
                willThrow(new ExceptionTemplate(ErrorCode.REFRESH_TOKEN_EXPIRED))
                        .given(authService).refreshSession("expired-token");

                // when & then
                mockMvc.perform(post("/api/v1/auth/refresh"))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED"));
        }

        // ===== GET /api/v1/auth/me =====

        @Test
        @DisplayName("GET /auth/me: 인증 없이 요청 시 UNAUTHORIZED 에러를 반환한다")
        void me_unauthenticated_returnsUnauthorized() throws Exception {
                // when & then — SecurityContext에 인증 없음 (anonymousUser)
                mockMvc.perform(get("/api/v1/auth/me"))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        // ===== helper =====

        private Map<String, String> validSignupBody() {
                Map<String, String> body = new HashMap<>();
                body.put("loginId", "testuser1");
                body.put("email", "test@example.com");
                body.put("password", "Test1234!");
                body.put("nickname", "테스터");
                return body;
        }
}

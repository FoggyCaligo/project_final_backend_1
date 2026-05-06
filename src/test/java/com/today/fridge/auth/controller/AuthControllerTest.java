package com.today.fridge.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.today.fridge.auth.dto.request.LoginRequest;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.exception.GlobalExceptionHandler;
import com.today.fridge.user.dto.request.SignupRequest;
//import com.today.fridge.user.entity.User;
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
//import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

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

        // ===== POST /api/v1/auth/signup (회원가입 — UserController에서 이동) =====

        @Test
        @DisplayName("POST /auth/signup: 정상 요청 시 success:true 응답을 반환한다")
        void signup_validRequest_returnsSuccess() throws Exception {
                // given
                Map<String, String> body = validSignupBody();
                willDoNothing().given(userService).signup(any(SignupRequest.class));

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
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

package com.today.fridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.exception.GlobalExceptionHandler;
import com.today.fridge.user.dto.request.SignupRequest;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = UserController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // ===== POST /signup =====

    @Test
    @DisplayName("POST /signup: 정상 요청 시 success:true 응답을 반환한다")
    void signup_validRequest_returnsSuccess() throws Exception {
        // given
        Map<String, String> body = validSignupBody();
        willDoNothing().given(userService).signup(any(SignupRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("POST /signup: loginId 공백 요청 시 success:false 응답을 반환한다")
    void signup_blankLoginId_returnsError() throws Exception {
        // given — loginId 공백
        Map<String, String> body = validSignupBody();
        body.put("loginId", "");

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /signup: loginId 중복 시 DUPLICATE_LOGIN_ID 에러 코드를 반환한다")
    void signup_duplicateLoginId_returnsErrorCode() throws Exception {
        // given
        Map<String, String> body = validSignupBody();
        willThrow(new ExceptionTemplate(ErrorCode.DUPLICATE_LOGIN_ID))
                .given(userService).signup(any(SignupRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    @DisplayName("POST /signup: email 중복 시 DUPLICATE_EMAIL 에러 코드를 반환한다")
    void signup_duplicateEmail_returnsErrorCode() throws Exception {
        // given
        Map<String, String> body = validSignupBody();
        willThrow(new ExceptionTemplate(ErrorCode.DUPLICATE_EMAIL))
                .given(userService).signup(any(SignupRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    // ===== GET /find-loginid =====

    @Test
    @DisplayName("GET /find-loginid: 등록된 이메일로 아이디를 반환한다")
    void findLoginId_existingEmail_returnsLoginId() throws Exception {
        // given
        given(userService.findLoginIdByEmail("test@example.com")).willReturn("testuser1");

        // when & then
        mockMvc.perform(get("/api/v1/users/find-loginid")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("testuser1"));
    }

    @Test
    @DisplayName("GET /find-loginid: 존재하지 않는 이메일 요청 시 USER_NOT_FOUND 에러 코드를 반환한다")
    void findLoginId_notFound_returnsErrorCode() throws Exception {
        // given
        given(userService.findLoginIdByEmail(anyString()))
                .willThrow(new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/users/find-loginid")
                        .param("email", "nobody@example.com"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
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

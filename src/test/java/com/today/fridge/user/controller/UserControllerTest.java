package com.today.fridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.exception.GlobalExceptionHandler;
import com.today.fridge.user.dto.request.PasswordChangeRequest;
import com.today.fridge.user.dto.request.ProfileUpdateRequest;
import com.today.fridge.user.dto.response.ProfileResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;

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

    // ===== GET /find-loginid =====
    // (POST /signup 테스트는 AuthControllerTest에 있음)

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

    // ===== GET /me/profile =====

    @Test
    @DisplayName("GET /me/profile: 인증된 사용자의 프로필을 반환한다")
    void getProfile_authenticated_returnsProfile() throws Exception {
        // given
        setAuth("testuser1");
        ProfileResponse profile = ProfileResponse.builder()
                .loginId("testuser1")
                .email("test@example.com")
                .nickname("테스터")
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .build();
        given(userService.getProfile("testuser1")).willReturn(profile);

        // when & then
        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("testuser1"))
                .andExpect(jsonPath("$.data.nickname").value("테스터"));
    }

    // ===== PATCH /me/profile =====

    @Test
    @DisplayName("PATCH /me/profile: 닉네임 변경 시 수정된 프로필을 반환한다")
    void updateProfile_validRequest_returnsUpdatedProfile() throws Exception {
        // given
        setAuth("testuser1");
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("새닉네임");

        ProfileResponse updated = ProfileResponse.builder()
                .loginId("testuser1")
                .email("test@example.com")
                .nickname("새닉네임")
                .status("ACTIVE")
                .build();
        given(userService.updateProfile(eq("testuser1"), any(ProfileUpdateRequest.class))).willReturn(updated);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("PATCH /me/profile: 닉네임 중복 시 DUPLICATE_NICKNAME 에러를 반환한다")
    void updateProfile_duplicateNickname_returnsError() throws Exception {
        // given
        setAuth("testuser1");
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("중복닉네임");
        willThrow(new ExceptionTemplate(ErrorCode.DUPLICATE_NICKNAME))
                .given(userService).updateProfile(eq("testuser1"), any(ProfileUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
    }

    // ===== PATCH /me/password =====

    @Test
    @DisplayName("PATCH /me/password: 정상 요청 시 비밀번호 변경 성공 응답을 반환한다")
    void changePassword_validRequest_returnsSuccess() throws Exception {
        // given
        setAuth("testuser1");
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("OldPass1!");
        req.setNewPassword("NewPass1!");
        willDoNothing().given(userService).changePassword(eq("testuser1"), any(PasswordChangeRequest.class));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));
    }

    @Test
    @DisplayName("PATCH /me/password: 현재 비밀번호 불일치 시 UNAUTHORIZED 에러를 반환한다")
    void changePassword_wrongCurrentPassword_returnsError() throws Exception {
        // given
        setAuth("testuser1");
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("WrongPass1!");
        req.setNewPassword("NewPass1!");
        willThrow(new ExceptionTemplate(ErrorCode.UNAUTHORIZED))
                .given(userService).changePassword(eq("testuser1"), any(PasswordChangeRequest.class));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ===== helper =====

    private void setAuth(String loginId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

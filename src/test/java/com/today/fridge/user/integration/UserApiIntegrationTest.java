package com.today.fridge.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.global.external.EmailService;
import com.today.fridge.user.dto.request.PasswordChangeRequest;
import com.today.fridge.user.dto.request.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * User API 통합 테스트
 * - H2 인메모리 DB, 전체 Spring 컨텍스트 로딩
 * - EmailService, RedisTemplate은 Mock 처리 (외부 의존성 제거)
 * - 인증이 필요한 API는 @WithMockUser를 이용해 SecurityContext에 사용자 주입
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    private long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users WHERE login_id IN (?, ?)", "testuser1", "testuser2");

        String passwordHash = passwordEncoder.encode("OldPass1!");
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, nickname) VALUES (?, ?, ?, ?)",
                "testuser1", "test@example.com", passwordHash, "테스터");
        userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE login_id = ?", Long.class, "testuser1");

        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, nickname) VALUES (?, ?, ?, ?)",
                "testuser2", "test2@example.com", passwordEncoder.encode("Pass1!22"), "다른유저");
    }

    // ============================================================
    // GET /api/v1/users/find-loginid
    // ============================================================

    @Test
    @DisplayName("[통합] GET /find-loginid: 등록된 이메일로 loginId를 반환한다")
    void findLoginId_registered_email_returns_loginId() throws Exception {
        mockMvc.perform(get("/api/v1/users/find-loginid")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("testuser1"));
    }

    @Test
    @DisplayName("[통합] GET /find-loginid: 미등록 이메일은 USER_NOT_FOUND 에러를 반환한다")
    void findLoginId_unknown_email_returns_not_found() throws Exception {
        mockMvc.perform(get("/api/v1/users/find-loginid")
                        .param("email", "nobody@example.com"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("[통합] GET /find-loginid: 이메일 형식이 아니면 400을 반환한다")
    void findLoginId_invalid_email_format_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/users/find-loginid")
                        .param("email", "not-an-email"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/v1/users/me/profile
    // ============================================================

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] GET /me/profile: 인증된 사용자의 프로필을 반환한다")
    void getProfile_authenticated_returns_profile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("testuser1"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("테스터"));
    }

    @Test
    @DisplayName("[통합] GET /me/profile: 미인증 요청은 UNAUTHORIZED를 반환한다")
    void getProfile_unauthenticated_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ============================================================
    // PATCH /api/v1/users/me/profile
    // ============================================================

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] PATCH /me/profile: 닉네임 변경 성공")
    void updateProfile_changes_nickname() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("새닉네임");

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
    }

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] PATCH /me/profile: 이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME을 반환한다")
    void updateProfile_duplicate_nickname_returns_error() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("다른유저"); // testuser2 닉네임과 동일

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
    }

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] PATCH /me/profile: 자기 자신의 닉네임으로 수정하면 정상 처리된다")
    void updateProfile_same_nickname_no_error() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("테스터"); // 현재 닉네임 그대로

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("테스터"));
    }

    // ============================================================
    // PATCH /api/v1/users/me/password
    // ============================================================

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] PATCH /me/password: 현재 비밀번호 일치 시 변경 성공")
    void changePassword_success() throws Exception {
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("OldPass1!");
        req.setNewPassword("NewPass2@");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));
    }

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] PATCH /me/password: 현재 비밀번호 불일치 시 UNAUTHORIZED 반환")
    void changePassword_wrong_current_password_returns_401() throws Exception {
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("WrongPass1!");
        req.setNewPassword("NewPass2@");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "testuser1")
    @DisplayName("[통합] PATCH /me/password: 새 비밀번호가 규칙에 맞지 않으면 INVALID_INPUT_VALUE 반환")
    void changePassword_invalid_new_password_returns_error() throws Exception {
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setCurrentPassword("OldPass1!");
        req.setNewPassword("short"); // 8자 미만

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}

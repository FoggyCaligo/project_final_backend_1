package com.today.fridge.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    // 32자 이상 비밀키 (HS256 최소 요구 사항)
    private static final String SECRET = "test-secret-key-for-jwt-unit-testing!!";

    private JwtProvider jwtProvider;
    private JwtProvider expiredJwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 3_600_000L, 1_209_600_000L);
        // validity < 0 → 발급 즉시 만료된 토큰 생성용
        expiredJwtProvider = new JwtProvider(SECRET, -1000L, -1000L);
    }

    // ===== 토큰 생성 =====

    @Test
    @DisplayName("createAccessToken: 비어있지 않은 JWT 문자열을 반환한다")
    void createAccessToken_returnsNonBlankString() {
        // when
        String token = jwtProvider.createAccessToken("testuser1");

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("createRefreshToken: 비어있지 않은 JWT 문자열을 반환한다")
    void createRefreshToken_returnsNonBlankString() {
        // when
        String token = jwtProvider.createRefreshToken("testuser1");

        // then
        assertThat(token).isNotBlank();
    }

    // ===== 토큰 검증 =====

    @Test
    @DisplayName("validateToken: 유효한 토큰은 true를 반환한다")
    void validateToken_validToken_returnsTrue() {
        // given
        String token = jwtProvider.createAccessToken("testuser1");

        // when & then
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: 만료된 토큰은 false를 반환한다")
    void validateToken_expiredToken_returnsFalse() {
        // given — expiredJwtProvider는 validity=-1000ms 이므로 생성 즉시 만료
        String expiredToken = expiredJwtProvider.createAccessToken("testuser1");

        // when & then
        assertThat(jwtProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken: 잘못된 형식의 토큰 문자열은 false를 반환한다")
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtProvider.validateToken("this.is.not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken: 빈 문자열은 false를 반환한다")
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtProvider.validateToken("")).isFalse();
    }

    // ===== 토큰 파싱 =====

    @Test
    @DisplayName("getLoginIdFromToken: 토큰에서 loginId를 정확히 추출한다")
    void getLoginIdFromToken_returnsCorrectLoginId() {
        // given
        String token = jwtProvider.createAccessToken("testuser1");

        // when
        String loginId = jwtProvider.getLoginIdFromToken(token);

        // then
        assertThat(loginId).isEqualTo("testuser1");
    }

    // ===== 쿠키 =====

    @Test
    @DisplayName("createTokenCookie: HttpOnly, Secure, 지정 maxAge로 쿠키를 생성한다")
    void createTokenCookie_hasCorrectAttributes() {
        // given
        String token = jwtProvider.createAccessToken("testuser1");

        // when
        ResponseCookie cookie = jwtProvider.createTokenCookie("accessToken", token, 3_600_000L);

        // then
        assertThat(cookie.getName()).isEqualTo("accessToken");
        assertThat(cookie.getValue()).isEqualTo(token);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("resolveTokenFromCookie: 요청 쿠키에서 지정 이름의 토큰을 추출한다")
    void resolveTokenFromCookie_findsCorrectToken() {
        // given
        String token = jwtProvider.createAccessToken("testuser1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new jakarta.servlet.http.Cookie("accessToken", token),
                new jakarta.servlet.http.Cookie("other", "value")
        );

        // when
        String resolved = jwtProvider.resolveTokenFromCookie(request, "accessToken");

        // then
        assertThat(resolved).isEqualTo(token);
    }

    @Test
    @DisplayName("resolveTokenFromCookie: 해당 이름의 쿠키가 없으면 null을 반환한다")
    void resolveTokenFromCookie_notFound_returnsNull() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        String resolved = jwtProvider.resolveTokenFromCookie(request, "accessToken");

        // then
        assertThat(resolved).isNull();
    }
}

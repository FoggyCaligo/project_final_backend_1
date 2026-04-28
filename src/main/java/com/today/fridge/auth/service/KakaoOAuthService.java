package com.today.fridge.auth.service;

import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class KakaoOAuthService {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthService.class);

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.kakao.rest-api-key}")
    private String restApiKey;

    @Value("${app.kakao.redirect-uri}")
    private String redirectUri;

    public KakaoOAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 인가 코드로 카카오 액세스 토큰 발급 */
    public String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", restApiKey);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, request, Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            throw new RuntimeException("카카오 액세스 토큰 발급 실패");
        }
        return (String) response.getBody().get("access_token");
    }

    /** 카카오 액세스 토큰으로 사용자 정보 조회 */
    @SuppressWarnings("unchecked")
    public KakaoUserProfile getKakaoUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                USER_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }

        Map<String, Object> body = response.getBody();
        String kakaoId = String.valueOf(body.get("id"));

        String email = null;
        String nickname = null;
        String profileImageUrl = null;

        Map<String, Object> account = (Map<String, Object>) body.get("kakao_account");
        if (account != null) {
            email = (String) account.get("email");
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            if (profile != null) {
                nickname = (String) profile.get("nickname");
                profileImageUrl = (String) profile.get("profile_image_url");
            }
        }

        log.info("[KakaoOAuth] 사용자 정보 조회 완료 kakaoId={}", kakaoId);
        return new KakaoUserProfile(kakaoId, email, nickname, profileImageUrl);
    }

    /** 카카오 사용자를 DB에서 조회하거나 신규 생성 */
    @Transactional
    public User findOrCreateUser(KakaoUserProfile profile) {
        String loginId = "kakao_" + profile.kakaoId();

        // 이미 카카오 로그인한 적 있으면 그대로 반환
        Optional<User> existing = userRepository.findByLoginId(loginId);
        if (existing.isPresent()) {
            log.info("[KakaoOAuth] 기존 카카오 사용자 반환 loginId={}", loginId);
            return existing.get();
        }

        // 동일 이메일 일반 계정이 있으면 해당 계정 반환 (계정 연동)
        if (profile.email() != null && !profile.email().isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(profile.email());
            if (byEmail.isPresent()) {
                log.info("[KakaoOAuth] 동일 이메일 기존 계정 반환 email={}", profile.email());
                return byEmail.get();
            }
        }

        // 닉네임 중복 처리: 겹치면 _kakaoId suffix 추가
        String nickname = resolveNickname(profile.nickname(), profile.kakaoId());

        User newUser = User.createFromKakao(profile.kakaoId(), profile.email(), nickname, profile.profileImageUrl());
        userRepository.save(newUser);
        log.info("[KakaoOAuth] 신규 카카오 사용자 생성 loginId={}", loginId);
        return newUser;
    }

    private String resolveNickname(String base, String kakaoId) {
        if (base == null || base.isBlank()) {
            return "카카오_" + kakaoId;
        }
        String trimmed = base.length() > 20 ? base.substring(0, 20) : base;
        if (!userRepository.existsByNickname(trimmed)) {
            return trimmed;
        }
        // 중복이면 suffix 추가
        String candidate = (trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed) + "_k";
        return userRepository.existsByNickname(candidate) ? "카카오_" + kakaoId : candidate;
    }

    public record KakaoUserProfile(String kakaoId, String email, String nickname, String profileImageUrl) {}
}

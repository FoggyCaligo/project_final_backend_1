package com.today.fridge.auth.kakao.client;

import com.today.fridge.auth.kakao.dto.KakaoTokenResponse;
import com.today.fridge.auth.kakao.dto.KakaoUserInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoApiClientImpl implements KakaoApiClient {

    private static final String TOKEN_URL     = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String LOGOUT_URL    = "https://kapi.kakao.com/v1/user/logout";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String redirectUri;

    public KakaoApiClientImpl(
            RestTemplate restTemplate,
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.redirect-uri}") String redirectUri) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    @Override
    public KakaoTokenResponse getToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            return restTemplate.postForEntity(
                    TOKEN_URL,
                    new HttpEntity<>(params, headers),
                    KakaoTokenResponse.class
            ).getBody();
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("유효하지 않은 카카오 인증 코드입니다");
        }
    }

    @Override
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            return restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoUserInfoResponse.class
            ).getBody();
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("카카오 사용자 정보 조회에 실패했습니다");
        }
    }

    @Override
    public void logout(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            restTemplate.postForEntity(
                    LOGOUT_URL,
                    new HttpEntity<>(headers),
                    Void.class
            );
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("카카오 로그아웃에 실패했습니다");
        }
    }
}

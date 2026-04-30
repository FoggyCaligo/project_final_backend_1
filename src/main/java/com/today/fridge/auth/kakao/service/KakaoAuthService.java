package com.today.fridge.auth.kakao.service;

import com.today.fridge.auth.dto.LoginResponse;
import com.today.fridge.auth.kakao.client.KakaoApiClient;
import com.today.fridge.auth.kakao.dto.KakaoTokenResponse;
import com.today.fridge.auth.kakao.dto.KakaoUserInfoResponse;
import com.today.fridge.global.security.JwtProvider;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoApiClient kakaoApiClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse kakaoLogin(String code) {
        KakaoTokenResponse tokenResponse = kakaoApiClient.getToken(code);
        KakaoUserInfoResponse userInfo = kakaoApiClient.getUserInfo(tokenResponse.getAccessToken());

        User user = userRepository.findByKakaoId(userInfo.getId())
                .orElseGet(() -> createKakaoUser(userInfo));

        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        return new LoginResponse(user.getUserId(), user.getNickname(), accessToken);
    }

    public void kakaoLogout(String kakaoAccessToken) {
        kakaoApiClient.logout(kakaoAccessToken);
    }

    private User createKakaoUser(KakaoUserInfoResponse userInfo) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .loginId("kakao_" + userInfo.getId())
                .kakaoId(userInfo.getId())
                .nickname(userInfo.getNickname())
                .status("active")
                .createdAt(now)
                .updatedAt(now)
                .build();
        return userRepository.save(user);
    }
}

package com.today.fridge.auth.kakao.client;

import com.today.fridge.auth.kakao.dto.KakaoTokenResponse;
import com.today.fridge.auth.kakao.dto.KakaoUserInfoResponse;

public interface KakaoApiClient {

    KakaoTokenResponse getToken(String code);

    KakaoUserInfoResponse getUserInfo(String accessToken);

    void logout(String accessToken);
}

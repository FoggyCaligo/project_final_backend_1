package com.today.fridge.auth.kakao.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KakaoTokenResponse {

    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private int expiresIn;
    private int refreshTokenExpiresIn;
}

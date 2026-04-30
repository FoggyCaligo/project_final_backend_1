package com.today.fridge.auth.kakao.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KakaoUserInfoResponse {

    private Long id;
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class KakaoAccount {

        private Profile profile;

        @Getter
        @NoArgsConstructor
        public static class Profile {
            private String nickname;
        }
    }

    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            String nickname = kakaoAccount.getProfile().getNickname();
            if (nickname != null && !nickname.isBlank()) {
                return nickname;
            }
        }
        return "카카오사용자" + id;
    }
}

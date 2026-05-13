package com.today.fridge.user.dto.response;

import com.today.fridge.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ProfileResponse {

    private String loginId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
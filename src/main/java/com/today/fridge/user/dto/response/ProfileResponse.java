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
    private Double heightCm;
    private Double weightKg;
    private Integer age;
    private String gender;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .status(user.getStatus())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .age(user.getAge())
                .gender(user.getGender())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

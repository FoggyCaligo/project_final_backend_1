package com.today.fridge.user.dto.response;

import com.today.fridge.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

@Getter
@Builder
public class ProfileResponse {

    private Long userId;
    private String loginId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Double heightCm;
    private Double weightKg;
    private Integer age;
    private String gender;
    private Boolean milkAllergy;
    private Boolean eggAllergy;
    private Boolean diet;
    private Boolean lowSodium;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProfileResponse from(User user) {
        return from(user, Set.of());
    }

    public static ProfileResponse from(User user, Collection<String> conditionCodes) {
        Set<String> codes = conditionCodes == null ? Set.of() : Set.copyOf(conditionCodes);

        return ProfileResponse.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .age(user.getAge())
                .gender(user.getGender())
                .milkAllergy(codes.contains("ALLERGY_MILK"))
                .eggAllergy(codes.contains("ALLERGY_EGG"))
                .diet(codes.contains("DIET_LOW_CALORIE"))
                .lowSodium(codes.contains("LOW_SODIUM"))
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

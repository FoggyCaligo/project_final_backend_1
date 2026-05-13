package com.today.fridge.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    private String nickname;

    @Size(max = 2048, message = "프로필 이미지 URL은 2048자를 초과할 수 없습니다.")
    private String profileImageUrl;

    private Double heightCm;

    private Double weightKg;

    private Integer age;

    @Size(max = 10, message = "성별은 10자를 초과할 수 없습니다.")
    private String gender;
}

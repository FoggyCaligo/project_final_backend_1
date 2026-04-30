package com.today.fridge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "아이디를 입력해주세요")
    @Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;
}

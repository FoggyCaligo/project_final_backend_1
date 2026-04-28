package com.today.fridge.user.controller;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.user.dto.request.SignupRequest;
import com.today.fridge.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 회원가입: loginId/email/password/nickname 검증 후 저장
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return ApiResponse.success(null, "회원가입이 완료되었습니다.");
    }

    // 비밀번호 찾기 (email): 이메일로 가입된 아이디 반환
    @GetMapping("/find-loginid")
    public ApiResponse<String> findLoginId(
            @RequestParam @NotBlank @Email(message = "올바른 이메일 형식이 아닙니다.") String email) {
        String loginId = userService.findLoginIdByEmail(email);
        return ApiResponse.success(loginId, "아이디를 찾았습니다.");
    }
}

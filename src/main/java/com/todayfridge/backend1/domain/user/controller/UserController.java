package com.todayfridge.backend1.domain.user.controller;

import com.todayfridge.backend1.domain.user.service.UserCommandService;
import com.todayfridge.backend1.domain.user.service.UserService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@Validated
public class UserController {
    private final UserService userService;
    private final UserCommandService userCommandService;

    public UserController(UserService userService, UserCommandService userCommandService) {
        this.userService = userService;
        this.userCommandService = userCommandService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(userService.profile(loginUser.getUserId()));
    }

    @PatchMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@CurrentUser LoginUser loginUser, @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("프로필이 수정되었습니다.",
                userCommandService.updateProfile(loginUser.getUserId(), request.nickname()));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@CurrentUser LoginUser loginUser, @RequestBody ChangePasswordRequest request) {
        userCommandService.changePassword(loginUser.getUserId(), request.currentPassword(), request.newPassword());
        return ApiResponse.success("비밀번호가 변경되었습니다.", null);
    }

    public record UpdateProfileRequest(@NotBlank String nickname) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
}

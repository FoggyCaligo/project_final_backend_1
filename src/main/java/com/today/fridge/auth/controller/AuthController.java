package com.today.fridge.auth.controller;

import com.today.fridge.auth.dto.LoginRequest;
import com.today.fridge.auth.dto.LoginResponse;
import com.today.fridge.auth.dto.SignupRequest;
import com.today.fridge.auth.email.EmailService;
import com.today.fridge.auth.service.AuthService;
import com.today.fridge.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    /** 아이디 중복확인 */
    @GetMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkDuplicate(
            @RequestParam String loginId) {
        boolean isDuplicate = authService.isDuplicateLoginId(loginId);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("isDuplicate", isDuplicate))
        );
    }

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다", null)
        );
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 이메일 인증 코드 발송 */
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @RequestParam String email) {
        emailService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다", null));
    }

    /** 이메일 인증 코드 확인 */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String email,
            @RequestParam String code) {
        emailService.verifyCode(email, code);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다", null));
    }
}

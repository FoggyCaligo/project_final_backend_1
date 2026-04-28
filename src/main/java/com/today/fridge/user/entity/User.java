package com.today.fridge.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verify_token", length = 255)
    private String emailVerifyToken;

    @Column(name = "email_verify_expiry")
    private LocalDateTime emailVerifyExpiry;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static User create(String loginId, String email, String passwordHash, String nickname) {
        User user = new User();
        user.loginId = loginId;
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.status = "PENDING_VERIFICATION";
        user.emailVerified = false;
        user.emailVerifyToken = UUID.randomUUID().toString();
        user.emailVerifyExpiry = LocalDateTime.now().plusHours(24);
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING_VERIFICATION";
        if (emailVerified == null) emailVerified = false;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 프로필 수정: 닉네임, 프로필 이미지 URL
    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    // 비밀번호 변경
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    // 최근 로그인 시간 갱신
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // 이메일 인증 완료
    public void verifyEmail() {
        this.emailVerified = true;
        this.status = "ACTIVE";
        this.emailVerifyToken = null;
        this.emailVerifyExpiry = null;
    }

    // 이메일 인증 토큰 재발급
    public void regenerateEmailVerifyToken() {
        this.emailVerifyToken = UUID.randomUUID().toString();
        this.emailVerifyExpiry = LocalDateTime.now().plusHours(24);
    }

    // 이메일 인증 토큰 유효성 확인
    public boolean isEmailVerifyTokenValid() {
        return this.emailVerifyToken != null
                && this.emailVerifyExpiry != null
                && LocalDateTime.now().isBefore(this.emailVerifyExpiry);
    }
}

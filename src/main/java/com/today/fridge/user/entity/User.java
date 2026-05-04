package com.today.fridge.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
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

    @Column(name = "height_cm", nullable = true)
    private Double heightCm;

    @Column(name = "weight_kg", nullable = true)
    private Double weightKg;

    @Column(name = "age", nullable = true)
    private Integer age;

    @Column(name = "gender", length = 10, nullable = true)
    private String gender;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verify_token", length = 255)
    private String emailVerifyToken;

    @Column(name = "email_verify_expiry")
    private OffsetDateTime emailVerifyExpiry;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // 카카오 소셜 로그인 사용자 생성 (비밀번호 로그인 불가 - 랜덤 UUID 저장)
    public static User createFromKakao(String kakaoId, String email, String nickname, String profileImageUrl) {
        User user = new User();
        user.loginId = "kakao_" + kakaoId;
        user.email = (email != null && !email.isBlank()) ? email : "kakao_" + kakaoId + "@kakao.oauth";
        user.passwordHash = UUID.randomUUID().toString();
        user.nickname = nickname;
        user.profileImageUrl = profileImageUrl;
        user.status = "ACTIVE";
        user.emailVerified = true;
        user.createdAt = OffsetDateTime.now();
        user.updatedAt = OffsetDateTime.now();
        return user;
    }

    public static User create(String loginId, String email, String passwordHash, String nickname) {
        User user = new User();
        user.loginId = loginId;
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.status = "PENDING_VERIFICATION";
        user.emailVerified = false;
        user.emailVerifyToken = UUID.randomUUID().toString();
        user.emailVerifyExpiry = OffsetDateTime.now().plusHours(24);
        user.createdAt = OffsetDateTime.now();
        user.updatedAt = OffsetDateTime.now();
        return user;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
        if (updatedAt == null)
            updatedAt = OffsetDateTime.now();
        if (status == null)
            status = "PENDING_VERIFICATION";
        if (emailVerified == null)
            emailVerified = false;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null)
            this.nickname = nickname;
        if (profileImageUrl != null)
            this.profileImageUrl = profileImageUrl;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    // 최근 로그인 시간 갱신
    public void updateLastLoginAt() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.status = "ACTIVE";
        this.emailVerifyToken = null;
        this.emailVerifyExpiry = null;
    }

    public void regenerateEmailVerifyToken() {
        this.emailVerifyToken = UUID.randomUUID().toString();
        this.emailVerifyExpiry = OffsetDateTime.now().plusHours(24);
    }

    public boolean isEmailVerifyTokenValid() {
        return this.emailVerifyToken != null
                && this.emailVerifyExpiry != null
                && OffsetDateTime.now().isBefore(this.emailVerifyExpiry);
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}

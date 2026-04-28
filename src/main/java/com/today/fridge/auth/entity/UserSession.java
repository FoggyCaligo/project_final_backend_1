package com.today.fridge.auth.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_session")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", length = 255)
    private String refreshTokenHash;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "last_ip", length = 64)
    private String lastIp;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static UserSession create(User user, String refreshTokenHash, LocalDateTime expiresAt) {
        UserSession session = new UserSession();
        session.user = user;
        session.refreshTokenHash = refreshTokenHash;
        session.expiresAt = expiresAt;
        session.createdAt = LocalDateTime.now();
        return session;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }
}

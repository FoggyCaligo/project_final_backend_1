package com.todayfridge.backend1.global.auth;

import com.todayfridge.backend1.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserPrincipal implements UserDetails {
    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final String nickname;
    private final String role;

    public CustomUserPrincipal(Long userId, String email, String passwordHash, String nickname, String role) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
    }

    public static CustomUserPrincipal from(User user) {
        return new CustomUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getNickname(), user.getRole());
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }
}

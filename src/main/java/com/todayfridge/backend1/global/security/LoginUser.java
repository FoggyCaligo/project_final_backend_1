package com.todayfridge.backend1.global.security;

public class LoginUser {
    private final Long userId;
    private final String email;
    private final String nickname;
    private final String role;

    public LoginUser(Long userId, String email, String nickname, String role) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getRole() { return role; }
}

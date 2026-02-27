package org.example.dto;

import java.util.List;

public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private List<String> roles;

    public LoginResponse(String token, Long userId, String username, List<String> roles) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}


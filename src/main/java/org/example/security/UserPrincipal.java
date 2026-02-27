package org.example.security;

import java.util.List;

public class UserPrincipal {
    private final Long id;
    private final String username;
    private final List<String> roles;

    public UserPrincipal(Long id, String username, List<String> roles) {
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}


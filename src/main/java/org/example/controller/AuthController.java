package org.example.controller;

import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.security.UserPrincipal;
import org.example.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.getUsername(), request.getPassword());
        User user = result.user();
        List<String> roleCodes = result.roles().stream().map(Role::getCode).collect(Collectors.toList());
        LoginResponse response = new LoginResponse(result.token(), user.getId(), user.getUsername(), roleCodes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return Map.of(
                "id", principal.getId(),
                "username", principal.getUsername(),
                "roles", principal.getRoles()
        );
    }
}


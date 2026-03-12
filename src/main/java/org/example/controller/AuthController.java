package org.example.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.security.JwtTokenService;
import org.example.security.UserPrincipal;
import org.example.service.AuthService;
import org.example.service.RedisService;
import org.springframework.http.HttpHeaders;
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
    private final JwtTokenService jwtTokenService;
    private final RedisService redisService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService, RedisService redisService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
        this.redisService = redisService;
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

    /** 登出：将当前 token 加入黑名单，剩余有效期内不可再使用 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtTokenService.parseToken(token);
                String jti = claims.getId();
                if (jti != null) {
                    long ttl = jwtTokenService.getRemainingSeconds(token);
                    redisService.addToBlacklist(jti, ttl);
                }
            } catch (Exception ignored) { }
        }
        return ResponseEntity.ok(Map.of("ok", true, "message", "已登出"));
    }
}


package org.example.service;

import org.example.entity.Role;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.mapper.RoleMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.UserRoleMapper;
import org.example.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserMapper userMapper,
                       UserRoleMapper userRoleMapper,
                       RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult login(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException(\"用户名和密码不能为空\");
        }
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException(\"用户名或密码错误\");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException(\"用户已被禁用\");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(\"用户名或密码错误\");
        }

        List<UserRole> userRoles = userRoleMapper.findByUserId(user.getId());
        List<Role> roles = new ArrayList<>();
        for (UserRole ur : userRoles) {
            Role role = roleMapper.findById(ur.getRoleId());
            if (role != null) {
                roles.add(role);
            }
        }

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), roles);
        return new LoginResult(user, roles, token);
    }

    public record LoginResult(User user, List<Role> roles, String token) {
    }
}


package org.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMinutes * 60 * 1000;
    }

    public String generateToken(Long userId, String username, List<Role> roles) {
        long now = System.currentTimeMillis();
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setId(jti)
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles.stream().map(Role::getCode).toArray(String[]::new))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 剩余有效秒数（用于登出时设置黑名单 TTL） */
    public long getRemainingSeconds(String token) {
        try {
            Claims claims = parseToken(token);
            Date exp = claims.getExpiration();
            if (exp == null) return 0;
            long sec = (exp.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, sec);
        } catch (Exception e) {
            return 0;
        }
    }
}


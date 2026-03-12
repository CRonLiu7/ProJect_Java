package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 阶段 4：Redis 封装——登录限流、JWT 黑名单、菜单缓存。
 */
@Service
public class RedisService {

    private static final String KEY_LOGIN_FAIL = "login:fail:";
    private static final String KEY_BLACKLIST = "blacklist:";
    private static final String KEY_MENUS = "user:menus:";

    private final StringRedisTemplate redis;
    private final int loginMaxAttempts;
    private final int loginLockMinutes;
    private final int menuCacheMinutes;

    public RedisService(
            StringRedisTemplate redis,
            @Value("${security.login-lock.max-attempts:5}") int loginMaxAttempts,
            @Value("${security.login-lock.lock-minutes:15}") int loginLockMinutes,
            @Value("${security.menu-cache-minutes:30}") int menuCacheMinutes
    ) {
        this.redis = redis;
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginLockMinutes = loginLockMinutes;
        this.menuCacheMinutes = menuCacheMinutes;
    }

    // ---------- 登录限流 ----------
    public boolean isLoginLocked(String username) {
        String key = KEY_LOGIN_FAIL + username;
        String v = redis.opsForValue().get(key);
        return v != null && Integer.parseInt(v) >= loginMaxAttempts;
    }

    public void recordLoginFailure(String username) {
        String key = KEY_LOGIN_FAIL + username;
        Long n = redis.opsForValue().increment(key);
        if (n != null && n == 1) {
            redis.expire(key, Duration.ofMinutes(loginLockMinutes));
        }
    }

    public void clearLoginFailures(String username) {
        redis.delete(KEY_LOGIN_FAIL + username);
    }

    // ---------- JWT 黑名单（登出） ----------
    public void addToBlacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) return;
        String key = KEY_BLACKLIST + jti;
        redis.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_BLACKLIST + jti));
    }

    // ---------- 菜单缓存 ----------
    public void setMenuCache(Long userId, String json) {
        redis.opsForValue().set(KEY_MENUS + userId, json, Duration.ofMinutes(menuCacheMinutes));
    }

    public String getMenuCache(Long userId) {
        return redis.opsForValue().get(KEY_MENUS + userId);
    }

    public void evictMenuCache(Long userId) {
        redis.delete(KEY_MENUS + userId);
    }
}

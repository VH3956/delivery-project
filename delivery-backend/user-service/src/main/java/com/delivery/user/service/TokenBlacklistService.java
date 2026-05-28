package com.delivery.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    // Spring Boot provides this template automatically to talk to Redis
    private final StringRedisTemplate redisTemplate;

    // Add token to blacklist with a specific Time-To-Live (TTL)
    public void addToBlacklist(String token, long expirationTimeInMillis) {
        long ttl = expirationTimeInMillis - System.currentTimeMillis();

        // Only blacklist if the token hasn't already expired
        if (ttl > 0) {
            // Save to Redis: Key = token, Value = "blacklisted", TTL = remaining time
            redisTemplate.opsForValue().set(token, "blacklisted", ttl, TimeUnit.MILLISECONDS);
        }
    }

    // Check if the token exists in Redis
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}
package com.werewolfengine.gateway.auth;

import com.werewolfengine.common.config.WerewolfAuthProperties;
import com.werewolfengine.common.redis.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class RedisTokenStore implements TokenStore {

    private final StringRedisTemplate redis;
    private final InMemoryTokenStore fallback;
    private final WerewolfAuthProperties authProperties;

    public RedisTokenStore(
            StringRedisTemplate redis,
            InMemoryTokenStore fallback,
            WerewolfAuthProperties authProperties
    ) {
        this.redis = redis;
        this.fallback = fallback;
        this.authProperties = authProperties;
    }

    @Override
    public Optional<Long> resolve(String opaque) {
        if (opaque == null || opaque.isBlank()) {
            return Optional.empty();
        }
        String userId = redis.opsForValue().get(RedisKeys.authToken(opaque));
        if (userId != null) {
            try {
                return Optional.of(Long.parseLong(userId));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        Optional<Long> dev = fallback.resolve(opaque);
        dev.ifPresent(id -> register(opaque, id, defaultTtl()));
        return dev;
    }

    @Override
    public void register(String opaque, long userId, Duration ttl) {
        Duration effective = ttl != null ? ttl : defaultTtl();
        redis.opsForValue().set(RedisKeys.authToken(opaque), String.valueOf(userId), effective);
    }

    private Duration defaultTtl() {
        return Duration.ofDays(Math.max(1, authProperties.getTokenTtlDays()));
    }
}

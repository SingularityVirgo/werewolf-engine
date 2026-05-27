package com.werewolfengine.gateway.auth;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process token store for tests and {@code redis-session=false}.
 * Also accepts numeric tokens as userId (Formal bot scripts).
 */
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    @Override
    public Optional<Long> resolve(String opaque) {
        if (opaque == null || opaque.isBlank()) {
            return Optional.empty();
        }
        Long stored = tokens.get(opaque);
        if (stored != null) {
            return Optional.of(stored);
        }
        try {
            return Optional.of(Long.parseLong(opaque.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public void register(String opaque, long userId, Duration ttl) {
        tokens.put(opaque, userId);
    }
}

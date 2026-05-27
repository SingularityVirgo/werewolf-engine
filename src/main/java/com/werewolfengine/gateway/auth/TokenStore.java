package com.werewolfengine.gateway.auth;

import java.time.Duration;
import java.util.Optional;

/** Opaque token → userId resolution (ADR-007 §4.2). */
public interface TokenStore {

    Optional<Long> resolve(String opaque);

    void register(String opaque, long userId, Duration ttl);
}

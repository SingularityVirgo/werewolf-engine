package com.werewolfengine.gateway.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenStoreTest {

    private final InMemoryTokenStore store = new InMemoryTokenStore();

    @Test
    void resolvesNumericTokenAsUserId() {
        assertThat(store.resolve("10001")).contains(10001L);
    }

    @Test
    void resolvesRegisteredOpaqueToken() {
        store.register("opaque-abc", 42L, Duration.ofDays(1));
        assertThat(store.resolve("opaque-abc")).contains(42L);
    }

    @Test
    void rejectsBlankToken() {
        assertThat(store.resolve("")).isEmpty();
        assertThat(store.resolve(null)).isEmpty();
    }
}

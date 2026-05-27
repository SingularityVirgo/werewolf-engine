package com.werewolfengine.gateway.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class SessionStoreConfiguration {

    @Bean
    @ConditionalOnProperty(name = "werewolf.persistence.redis-session", havingValue = "false", matchIfMissing = true)
    SessionStore inMemorySessionStore() {
        return new InMemorySessionStore();
    }

    @Bean
    @ConditionalOnProperty(name = "werewolf.persistence.redis-session", havingValue = "true")
    SessionStore redisSessionStore(StringRedisTemplate redis) {
        return new RedisSessionStore(redis, new InMemorySessionStore());
    }
}

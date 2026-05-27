package com.werewolfengine.gateway.auth;

import com.werewolfengine.common.config.WerewolfAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AuthConfiguration {

    @Bean
    @ConditionalOnProperty(name = "werewolf.persistence.redis-session", havingValue = "false", matchIfMissing = true)
    TokenStore inMemoryTokenStore() {
        return new InMemoryTokenStore();
    }

    @Bean
    @ConditionalOnProperty(name = "werewolf.persistence.redis-session", havingValue = "true")
    TokenStore redisTokenStore(StringRedisTemplate redis, WerewolfAuthProperties authProperties) {
        return new RedisTokenStore(redis, new InMemoryTokenStore(), authProperties);
    }
}

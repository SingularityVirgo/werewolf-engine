package com.werewolfengine.room.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomPersistenceConfiguration {

    @Bean
    @ConditionalOnMissingBean(RoomLobbyPersistence.class)
    RoomLobbyPersistence roomLobbyPersistenceFallback() {
        return RoomLobbyPersistence.NO_OP;
    }
}

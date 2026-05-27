package com.werewolfengine.room.persistence;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "werewolf.persistence.mysql-room", havingValue = "true")
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@EnableJpaRepositories(basePackages = {
        "com.werewolfengine.room.persistence",
        "com.werewolfengine.game.persistence"
})
public class JpaPersistenceConfiguration {
}

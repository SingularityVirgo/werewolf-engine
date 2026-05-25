package com.werewolfengine.game.config;

import com.werewolfengine.game.sync.PhaseCountdown;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PhaseCountdownSettings {

    @Value("${werewolf.game.phase-countdown-enabled:true}")
    private boolean phaseCountdownEnabled;

    @PostConstruct
    void apply() {
        PhaseCountdown.setEnabled(phaseCountdownEnabled);
    }
}

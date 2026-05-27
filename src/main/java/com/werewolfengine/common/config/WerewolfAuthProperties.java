package com.werewolfengine.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "werewolf.auth")
public class WerewolfAuthProperties {

    private int tokenTtlDays = 7;

    public int getTokenTtlDays() {
        return tokenTtlDays;
    }

    public void setTokenTtlDays(int tokenTtlDays) {
        this.tokenTtlDays = tokenTtlDays;
    }
}

package com.werewolfengine.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "werewolf.gateway")
public class WerewolfGatewayProperties {

    private int reconnectGraceSeconds = 30;

    public int getReconnectGraceSeconds() {
        return reconnectGraceSeconds;
    }

    public void setReconnectGraceSeconds(int reconnectGraceSeconds) {
        this.reconnectGraceSeconds = reconnectGraceSeconds;
    }
}

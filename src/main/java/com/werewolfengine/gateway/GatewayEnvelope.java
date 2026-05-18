package com.werewolfengine.gateway;

import java.util.Map;

public record GatewayEnvelope(
        String type,
        Map<String, Object> payload
) {
}

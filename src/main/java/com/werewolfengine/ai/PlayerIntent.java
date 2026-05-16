package com.werewolfengine.ai;

import com.werewolfengine.game.model.GameActionType;

public record PlayerIntent(
        GameActionType action,
        Integer target,
        String reason
) {
}

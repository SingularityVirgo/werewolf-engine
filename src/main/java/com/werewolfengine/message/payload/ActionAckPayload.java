package com.werewolfengine.message.payload;

import com.werewolfengine.game.model.ActionErrorCode;
import com.werewolfengine.game.model.GamePhase;

public record ActionAckPayload(
        boolean success,
        String message,
        ActionErrorCode code,
        GamePhase serverPhase,
        String playerSubState
) {
}

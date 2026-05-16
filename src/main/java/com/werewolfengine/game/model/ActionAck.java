package com.werewolfengine.game.model;

public record ActionAck(
        boolean success,
        String message,
        ActionErrorCode code,
        GamePhase serverPhase,
        String playerSubState
) {
    public static ActionAck ok(String message, GamePhase serverPhase, String playerSubState) {
        return new ActionAck(true, message, null, serverPhase, playerSubState);
    }

    public static ActionAck fail(ActionErrorCode code, String message, GamePhase serverPhase) {
        return new ActionAck(false, message, code, serverPhase, null);
    }
}

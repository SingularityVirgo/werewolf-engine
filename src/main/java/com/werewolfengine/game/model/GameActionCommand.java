package com.werewolfengine.game.model;

public record GameActionCommand(
        int playerId,
        GameActionType action,
        Integer target,
        GamePhase clientPhase
) {
}

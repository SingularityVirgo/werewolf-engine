package com.werewolfengine.game.model;

public enum Role {
    WEREWOLF,
    VILLAGER,
    IDIOT,
    SEER,
    WITCH,
    HUNTER;

    public boolean isWolf() {
        return this == WEREWOLF;
    }
}

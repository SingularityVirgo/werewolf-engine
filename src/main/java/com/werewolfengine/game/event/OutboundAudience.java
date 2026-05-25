package com.werewolfengine.game.event;

/** Who should receive an outbound WS message (PRD §4.6.5). */
public enum OutboundAudience {
    /** Public game events — all connected seats (alive or dead). */
    PUBLIC,
    /** Wolf channel — connected alive werewolves only. */
    WOLF_ONLY,
    /** Day chat — connected alive players only. */
    ALIVE_CONNECTED,
    /** Game over recap — every connected seat. */
    ALL_CONNECTED
}

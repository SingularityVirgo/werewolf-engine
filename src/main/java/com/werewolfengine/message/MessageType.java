package com.werewolfengine.message;

/** PRD §4.6 — wire message types (subset for Week1). */
public enum MessageType {
    CONNECTED,
    PHASE_SYNC,
    ACTION_ACK,
    GAME_EVENT,
    CHAT_BROADCAST,
    GAME_OVER,
    ERROR,
    GAME_ACTION
}

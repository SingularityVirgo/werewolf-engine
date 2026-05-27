package com.werewolfengine.game.model;

/** WS seat connection state during PLAYING (ADR-007 §5.2). Not persisted to MySQL. */
public enum ConnectionState {
    ONLINE,
    GRACE,
    AI_HOSTED
}

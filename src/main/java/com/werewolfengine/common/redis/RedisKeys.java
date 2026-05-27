package com.werewolfengine.common.redis;

/** Central Redis key patterns (ADR-007 §4.2). */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static String authToken(String opaque) {
        return "werewolf:auth:token:" + opaque;
    }

    public static String wsConn(String roomId, int playerId) {
        return "werewolf:ws:conn:" + roomId + ":" + playerId;
    }

    public static String wsGrace(String roomId, int playerId) {
        return "werewolf:ws:grace:" + roomId + ":" + playerId;
    }

    public static String roomPlayers(String roomId) {
        return "werewolf:room:" + roomId + ":players";
    }

    public static String roomKeyPrefix(String roomId) {
        return "werewolf:*:" + roomId + ":*";
    }
}

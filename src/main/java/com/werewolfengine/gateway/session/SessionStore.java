package com.werewolfengine.gateway.session;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/** WS session bindings and grace window (ADR-007 §4.2). */
public interface SessionStore {

    void bindConnection(String roomId, int playerId, String sessionId);

    void setGrace(String roomId, int playerId, long epochMs, Duration ttl);

    boolean clearGrace(String roomId, int playerId);

    boolean isInGrace(String roomId, int playerId);

    Optional<String> getBoundSessionId(String roomId, int playerId);

    void addRoomPlayer(String roomId, int playerId);

    void removeRoomPlayer(String roomId, int playerId);

    Set<Integer> roomPlayerIds(String roomId);

    void cleanupRoom(String roomId);
}

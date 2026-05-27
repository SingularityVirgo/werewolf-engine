package com.werewolfengine.gateway.session;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore implements SessionStore {

    private final Map<String, String> connBySeat = new ConcurrentHashMap<>();
    private final Map<String, Long> graceDeadlineBySeat = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> playersByRoom = new ConcurrentHashMap<>();

    @Override
    public void bindConnection(String roomId, int playerId, String sessionId) {
        connBySeat.put(seatKey(roomId, playerId), sessionId);
    }

    @Override
    public void setGrace(String roomId, int playerId, long epochMs, Duration ttl) {
        graceDeadlineBySeat.put(seatKey(roomId, playerId), epochMs + ttl.toMillis());
    }

    @Override
    public boolean clearGrace(String roomId, int playerId) {
        return graceDeadlineBySeat.remove(seatKey(roomId, playerId)) != null;
    }

    @Override
    public boolean isInGrace(String roomId, int playerId) {
        Long deadline = graceDeadlineBySeat.get(seatKey(roomId, playerId));
        if (deadline == null) {
            return false;
        }
        if (System.currentTimeMillis() >= deadline) {
            graceDeadlineBySeat.remove(seatKey(roomId, playerId));
            return false;
        }
        return true;
    }

    @Override
    public Optional<String> getBoundSessionId(String roomId, int playerId) {
        return Optional.ofNullable(connBySeat.get(seatKey(roomId, playerId)));
    }

    @Override
    public void addRoomPlayer(String roomId, int playerId) {
        playersByRoom.computeIfAbsent(roomId, id -> ConcurrentHashMap.newKeySet()).add(playerId);
    }

    @Override
    public void removeRoomPlayer(String roomId, int playerId) {
        Set<Integer> seats = playersByRoom.get(roomId);
        if (seats != null) {
            seats.remove(playerId);
        }
    }

    @Override
    public Set<Integer> roomPlayerIds(String roomId) {
        Set<Integer> seats = playersByRoom.get(roomId);
        return seats == null ? Set.of() : Set.copyOf(seats);
    }

    @Override
    public void cleanupRoom(String roomId) {
        playersByRoom.remove(roomId);
        String prefix = roomId + ":";
        connBySeat.keySet().removeIf(k -> k.startsWith(prefix));
        graceDeadlineBySeat.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private static String seatKey(String roomId, int playerId) {
        return roomId + ":" + playerId;
    }
}

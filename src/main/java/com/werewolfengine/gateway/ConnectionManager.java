package com.werewolfengine.gateway;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final Map<String, ConnectionRecord> bySessionId = new ConcurrentHashMap<>();
    private final Map<String, ConnectionRecord> bySeatKey = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        bySessionId.put(session.getId(), new ConnectionRecord(session.getId(), session, null, null));
    }

    public void bind(String sessionId, String roomId, Integer seatId) {
        ConnectionRecord current = bySessionId.get(sessionId);
        if (current == null) {
            return;
        }
        ConnectionRecord updated = new ConnectionRecord(sessionId, current.session(), roomId, seatId);
        bySessionId.put(sessionId, updated);
        if (roomId != null && seatId != null) {
            bySeatKey.put(seatKey(roomId, seatId), updated);
        }
    }

    public Optional<ConnectionRecord> findBySeat(String roomId, int seatId) {
        return Optional.ofNullable(bySeatKey.get(seatKey(roomId, seatId)));
    }

    public void remove(String sessionId) {
        ConnectionRecord record = bySessionId.remove(sessionId);
        if (record != null && record.roomId() != null && record.seatId() != null) {
            bySeatKey.remove(seatKey(record.roomId(), record.seatId()));
        }
    }

    private static String seatKey(String roomId, int seatId) {
        return roomId + ":" + seatId;
    }

    public record ConnectionRecord(
            String sessionId,
            WebSocketSession session,
            String roomId,
            Integer seatId
    ) {
    }
}

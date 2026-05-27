package com.werewolfengine.gateway;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {

    private final Map<String, ConnectionRecord> bySessionId = new ConcurrentHashMap<>();
    private final Map<String, ConnectionRecord> bySeatKey = new ConcurrentHashMap<>();

    public void remove(String sessionId) {
        ConnectionRecord record = bySessionId.remove(sessionId);
        if (record != null && record.roomId() != null && record.seatId() != null) {
            bySeatKey.remove(seatKey(record.roomId(), record.seatId()));
        }
    }

    /** Keeps seat occupancy while WS is down during grace (ADR-007 §5.3-B). */
    public void graceUnlink(String sessionId) {
        ConnectionRecord record = bySessionId.remove(sessionId);
        if (record == null || record.roomId() == null || record.seatId() == null) {
            return;
        }
        bySeatKey.put(
                seatKey(record.roomId(), record.seatId()),
                new ConnectionRecord(record.sessionId(), null, record.roomId(), record.seatId(), record.userId())
        );
    }

    public void rebindSession(WebSocketSession session, String roomId, int seatId) {
        Long userId = findBySession(session.getId()).map(ConnectionRecord::userId).orElse(null);
        bySeatKey.remove(seatKey(roomId, seatId));
        ConnectionRecord updated = new ConnectionRecord(session.getId(), session, roomId, seatId, userId);
        bySessionId.put(session.getId(), updated);
        bySeatKey.put(seatKey(roomId, seatId), updated);
    }

    public void register(WebSocketSession session, Long userId) {
        bySessionId.put(session.getId(), new ConnectionRecord(session.getId(), session, null, null, userId));
    }

    public void bind(String sessionId, String roomId, Integer seatId) {
        ConnectionRecord current = bySessionId.get(sessionId);
        if (current == null) {
            return;
        }
        if (current.roomId() != null && current.seatId() != null) {
            bySeatKey.remove(seatKey(current.roomId(), current.seatId()));
        }
        ConnectionRecord updated = new ConnectionRecord(
                sessionId, current.session(), roomId, seatId, current.userId());
        bySessionId.put(sessionId, updated);
        if (roomId != null && seatId != null) {
            bySeatKey.put(seatKey(roomId, seatId), updated);
        }
    }

    public Optional<ConnectionRecord> findBySeat(String roomId, int seatId) {
        return Optional.ofNullable(bySeatKey.get(seatKey(roomId, seatId)));
    }

    public Optional<ConnectionRecord> findBySession(String sessionId) {
        return Optional.ofNullable(bySessionId.get(sessionId));
    }

    public List<Integer> connectedSeatIds(String roomId) {
        List<Integer> seats = new ArrayList<>();
        for (ConnectionRecord record : bySeatKey.values()) {
            if (!roomId.equals(record.roomId()) || record.seatId() == null) {
                continue;
            }
            WebSocketSession session = record.session();
            if (session != null && session.isOpen()) {
                seats.add(record.seatId());
            }
        }
        return seats;
    }

    /** Drops all seat bindings for a dissolved room (sessions stay open). */
    public void removeRoom(String roomId) {
        List<String> sessionIds = new ArrayList<>();
        for (ConnectionRecord record : bySessionId.values()) {
            if (roomId.equals(record.roomId())) {
                sessionIds.add(record.sessionId());
            }
        }
        for (String sessionId : sessionIds) {
            remove(sessionId);
        }
    }

    private static String seatKey(String roomId, int seatId) {
        return roomId + ":" + seatId;
    }

    public record ConnectionRecord(
            String sessionId,
            WebSocketSession session,
            String roomId,
            Integer seatId,
            Long userId
    ) {
        ConnectionRecord(String sessionId, WebSocketSession session, String roomId, Integer seatId) {
            this(sessionId, session, roomId, seatId, null);
        }
    }
}

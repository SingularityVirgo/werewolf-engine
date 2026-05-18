package com.werewolfengine.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolfengine.room.RoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageRouter router;
    private final RoomService roomService;
    private final ConnectionManager connectionManager = new ConnectionManager();

    public GameWebSocketHandler(MessageRouter router, RoomService roomService) {
        this.router = router;
        this.roomService = roomService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connectionManager.register(session);
        send(session, Map.of(
                "type", "CONNECTED",
                "payload", Map.of("sessionId", session.getId())
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> envelope = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
            String type = String.valueOf(envelope.get("type"));
            Map<String, Object> payload = castPayload(envelope.get("payload"));
            String roomId = payload != null && payload.get("roomId") != null ? String.valueOf(payload.get("roomId")) : null;
            if ("JOIN_ROOM".equals(type) && roomId != null) {
                Integer seatId = payload.get("seatId") == null ? null : ((Number) payload.get("seatId")).intValue();
                if (seatId == null) {
                    sendError(session, "seatId required");
                    return;
                }
                connectionManager.bind(session.getId(), roomId, seatId);
                Long userId = payload.get("userId") == null ? null : ((Number) payload.get("userId")).longValue();
                if (userId != null) {
                    roomService.joinRoom(roomId, seatId, userId);
                }
                send(session, Map.of(
                        "type", "JOIN_ROOM",
                        "payload", Map.of("roomId", roomId, "seatId", seatId)
                ));
                return;
            }
            if ("READY".equals(type) && roomId != null) {
                Integer seatId = payload.get("seatId") == null ? null : ((Number) payload.get("seatId")).intValue();
                if (seatId == null) {
                    sendError(session, "seatId required");
                    return;
                }
                boolean ready = payload.get("ready") == null || Boolean.parseBoolean(String.valueOf(payload.get("ready")));
                RoomService.SeatSnapshot seat = roomService.setReady(roomId, seatId, ready);
                send(session, Map.of(
                        "type", "READY",
                        "payload", Map.of(
                                "roomId", seat.roomId(),
                                "seatId", seat.seatId(),
                                "ready", seat.ready(),
                                "phase", seat.phase().name()
                        )
                ));
                return;
            }
            if (roomId == null) {
                sendError(session, "roomId required");
                return;
            }
            Map<String, Object> response = router.handle(roomId, type, payload == null ? Map.of() : payload);
            send(session, response);
        } catch (Exception e) {
            sendError(session, e.getMessage() == null ? "gateway error" : e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectionManager.remove(session.getId());
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        send(session, Map.of("type", "ERROR", "payload", Map.of("message", message)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}

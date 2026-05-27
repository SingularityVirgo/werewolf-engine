package com.werewolfengine.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.gateway.session.ReconnectService;
import com.werewolfengine.room.RoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageRouter router;
    private final RoomService roomService;
    private final GameEngineService gameEngine;
    private final ConnectionManager connectionManager;
    private final WsPushService wsPushService;
    private final RoomPhaseTickScheduler phaseTickScheduler;
    private final ReconnectService reconnectService;

    public GameWebSocketHandler(
            MessageRouter router,
            RoomService roomService,
            GameEngineService gameEngine,
            ConnectionManager connectionManager,
            WsPushService wsPushService,
            RoomPhaseTickScheduler phaseTickScheduler,
            ReconnectService reconnectService
    ) {
        this.router = router;
        this.roomService = roomService;
        this.gameEngine = gameEngine;
        this.connectionManager = connectionManager;
        this.wsPushService = wsPushService;
        this.phaseTickScheduler = phaseTickScheduler;
        this.reconnectService = reconnectService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = sessionUserId(session);
        connectionManager.register(session, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.getId());
        if (userId != null) {
            payload.put("userId", userId);
        }
        wsPushService.sendEnvelope(session, Map.of("type", "CONNECTED", "payload", payload));
        if (userId != null) {
            reconnectService.tryReconnect(session, userId);
        }
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
                Long payloadUserId = payload.get("userId") == null ? null : ((Number) payload.get("userId")).longValue();
                Long sessionUserId = sessionUserId(session);
                if (sessionUserId != null && payloadUserId != null && !sessionUserId.equals(payloadUserId)) {
                    sendError(session, "token userId mismatch");
                    return;
                }
                Long userId = payloadUserId != null ? payloadUserId : sessionUserId;
                try {
                    RoomService.SeatSnapshot joined = roomService.joinRoom(roomId, seatId, userId);
                    int boundSeat = joined.seatId();
                    reconnectService.bindJoinedSeat(session, roomId, boundSeat);
                    wsPushService.sendEnvelope(session, Map.of(
                            "type", "JOIN_ROOM",
                            "payload", Map.of(
                                    "roomId", roomId,
                                    "seatId", boundSeat,
                                    "playerId", boundSeat
                            )
                    ));
                    wsPushService.pushPhaseSync(roomId, boundSeat);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    sendError(session, e.getMessage());
                }
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
                wsPushService.sendEnvelope(session, Map.of(
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
            if ("PHASE_TICK".equals(type)) {
                if (roomId == null) {
                    sendError(session, "roomId required");
                    return;
                }
                wsPushService.sendEnvelope(session, Map.of(
                        "type", "PHASE_TICK",
                        "payload", phaseTickScheduler.tickOnceResponse(roomId)
                ));
                return;
            }
            if (roomId == null) {
                sendError(session, "roomId required");
                return;
            }
            Map<String, Object> response = router.handle(roomId, type, payload == null ? Map.of() : payload);
            wsPushService.sendEnvelope(session, response);
        } catch (Exception e) {
            sendError(session, e.getMessage() == null ? "gateway error" : e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectionManager.findBySession(session.getId()).ifPresentOrElse(record -> {
            if (record.roomId() != null && record.seatId() != null && record.userId() != null) {
                try {
                    GameRoomState room = gameEngine.getRoomState(record.roomId());
                    if (room.getStatus() == RoomStatus.PLAYING) {
                        reconnectService.onPlayingDisconnect(
                                session.getId(),
                                record.roomId(),
                                record.seatId(),
                                record.userId()
                        );
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                    // room evicted
                }
            }
            connectionManager.remove(session.getId());
        }, () -> connectionManager.remove(session.getId()));
    }

    private static Long sessionUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(WebSocketAuthHandshakeInterceptor.ATTR_USER_ID);
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private void sendError(WebSocketSession session, String message) {
        wsPushService.sendEnvelope(session, Map.of("type", "ERROR", "payload", Map.of("message", message)));
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

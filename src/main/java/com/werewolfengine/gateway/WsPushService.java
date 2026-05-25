package com.werewolfengine.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.event.OutboundAudience;
import com.werewolfengine.game.event.OutboundMessage;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.view.SeatVisibility;
import com.werewolfengine.message.MessageType;
import com.werewolfengine.message.payload.PhaseSyncPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbound WS push (ADR-005): per-seat {@link PhaseSyncPayload}, {@link OutboundMessage} queue drain.
 */
@Service
public class WsPushService {

    private static final Logger log = LoggerFactory.getLogger(WsPushService.class);

    private final ConnectionManager connectionManager;
    private final GameEngineService gameEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WsPushService(ConnectionManager connectionManager, GameEngineService gameEngine) {
        this.connectionManager = connectionManager;
        this.gameEngine = gameEngine;
    }

    public void pushPhaseSync(String roomId, int seatId) {
        PhaseSyncPayload sync = gameEngine.buildPhaseSync(roomId, seatId);
        sendToSeat(roomId, seatId, MessageType.PHASE_SYNC.name(), phasePayload(seatId, sync));
    }

    public void pushPhaseSyncToConnected(String roomId) {
        List<Integer> seats = connectionManager.connectedSeatIds(roomId);
        for (int seatId : seats) {
            pushPhaseSync(roomId, seatId);
        }
    }

    /**
     * Delivers queued {@link OutboundMessage}s from the game room (PRD §4.6.4).
     */
    public void flushOutbound(String roomId) {
        GameRoomState room;
        try {
            room = gameEngine.getRoomState(roomId);
        } catch (IllegalArgumentException e) {
            return;
        }
        List<OutboundMessage> messages = room.drainOutbound();
        for (OutboundMessage message : messages) {
            deliver(room, message);
        }
    }

    void deliver(GameRoomState room, OutboundMessage message) {
        String roomId = room.getRoomId();
        List<Integer> seats = connectionManager.connectedSeatIds(roomId);
        for (int seatId : seats) {
            if (!shouldReceive(room, seatId, message.audience())) {
                continue;
            }
            String wireType = wireType(message);
            Map<String, Object> payload = buildPayload(message);
            sendToSeat(roomId, seatId, wireType, payload);
        }
    }

    static String wireType(OutboundMessage message) {
        return switch (message.kind()) {
            case GAME_EVENT -> MessageType.GAME_EVENT.name();
            case CHAT_BROADCAST -> MessageType.CHAT_BROADCAST.name();
            case GAME_OVER -> MessageType.GAME_OVER.name();
        };
    }

    static Map<String, Object> buildPayload(OutboundMessage message) {
        return switch (message.kind()) {
            case GAME_EVENT -> {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("eventType", message.eventType());
                body.put("data", message.data());
                yield body;
            }
            case CHAT_BROADCAST, GAME_OVER -> Map.copyOf(message.data());
        };
    }

    static boolean shouldReceive(GameRoomState room, int seatId, OutboundAudience audience) {
        PlayerState viewer = room.getPlayer(seatId);
        if (viewer == null) {
            return false;
        }
        return switch (audience) {
            case PUBLIC, ALL_CONNECTED -> true;
            case WOLF_ONLY -> viewer.isAlive() && SeatVisibility.isWolf(room, seatId);
            case ALIVE_CONNECTED -> viewer.isAlive();
        };
    }

    public void sendToSeat(String roomId, int seatId, String type, Map<String, Object> payload) {
        connectionManager.findBySeat(roomId, seatId).ifPresent(record -> {
            WebSocketSession session = record.session();
            if (session != null && session.isOpen()) {
                sendEnvelope(session, envelope(type, payload));
            }
        });
    }

    public void sendEnvelope(WebSocketSession session, Map<String, Object> envelope) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
        } catch (IOException e) {
            log.warn("WS send failed sessionId={}: {}", session.getId(), e.getMessage());
        }
    }

    static Map<String, Object> phasePayload(int seatId, PhaseSyncPayload sync) {
        return Map.of("seatId", seatId, "phaseSync", sync);
    }

    private static Map<String, Object> envelope(String type, Map<String, Object> payload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("payload", payload);
        return body;
    }
}

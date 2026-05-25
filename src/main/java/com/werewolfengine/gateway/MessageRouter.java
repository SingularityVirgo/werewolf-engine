package com.werewolfengine.gateway;

import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.model.GameActionCommand;
import com.werewolfengine.message.MessageType;
import com.werewolfengine.message.payload.ActionAckPayload;
import com.werewolfengine.message.payload.PhaseSyncPayload;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageRouter {

    private final GameEngineService gameEngine;
    private final RoomExecutionGuard roomGuard;
    private final WsPushService wsPushService;

    public MessageRouter(
            GameEngineService gameEngine,
            RoomExecutionGuard roomGuard,
            WsPushService wsPushService
    ) {
        this.gameEngine = gameEngine;
        this.roomGuard = roomGuard;
        this.wsPushService = wsPushService;
    }

    public Map<String, Object> handle(String roomId, String type, Map<String, Object> payload) {
        MessageType messageType;
        try {
            messageType = MessageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return envelope(MessageType.ERROR.name(), Map.of("message", "unsupported message: " + type));
        }
        return switch (messageType) {
            case CONNECTED -> envelope(messageType.name(), Map.of("roomId", roomId));
            case GAME_ACTION -> handleGameAction(roomId, payload);
            case CHAT_MESSAGE -> handleChatMessage(roomId, payload);
            case PHASE_SYNC -> handlePhaseSync(roomId, payload);
            default -> envelope(MessageType.ERROR.name(), Map.of("message", "unsupported message: " + type));
        };
    }

    private Map<String, Object> handleGameAction(String roomId, Map<String, Object> payload) {
        int playerId = ((Number) payload.get("playerId")).intValue();
        GameActionCommand command = new GameActionCommand(
                playerId,
                com.werewolfengine.game.model.GameActionType.valueOf(String.valueOf(payload.get("action"))),
                payload.get("target") == null ? null : ((Number) payload.get("target")).intValue(),
                payload.get("phase") == null ? null : com.werewolfengine.game.model.GamePhase.valueOf(String.valueOf(payload.get("phase"))),
                payload.get("content") == null ? null : String.valueOf(payload.get("content"))
        );
        GameEngineService.ActionResult result = roomGuard.execute(
                roomId,
                () -> gameEngine.submitAction(roomId, command)
        );
        if (!result.phaseSyncs().isEmpty()) {
            wsPushService.pushPhaseSyncToConnected(roomId);
        }
        wsPushService.flushOutbound(roomId);
        return envelope(MessageType.ACTION_ACK.name(), ackPayload(roomId, playerId, result.ack(), result.phaseSyncs()));
    }

    private Map<String, Object> handleChatMessage(String roomId, Map<String, Object> payload) {
        int playerId = ((Number) payload.get("playerId")).intValue();
        String scope = String.valueOf(payload.get("scope"));
        String content = payload.get("content") == null ? null : String.valueOf(payload.get("content"));
        var result = roomGuard.execute(
                roomId,
                () -> gameEngine.submitChatMessage(roomId, playerId, scope, content)
        );
        if (!result.phaseSyncs().isEmpty()) {
            wsPushService.pushPhaseSyncToConnected(roomId);
        }
        wsPushService.flushOutbound(roomId);
        return envelope(MessageType.ACTION_ACK.name(), Map.of("ack", result.ack()));
    }

    private Map<String, Object> handlePhaseSync(String roomId, Map<String, Object> payload) {
        int seatId = ((Number) payload.get("seatId")).intValue();
        PhaseSyncPayload sync = gameEngine.buildPhaseSync(roomId, seatId);
        return envelope(MessageType.PHASE_SYNC.name(), WsPushService.phasePayload(seatId, sync));
    }

    private Map<String, Object> ackPayload(String roomId, int actorSeatId, ActionAckPayload ack, List<PhaseSyncPayload> phaseSyncs) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ack", ack);
        body.put("phaseSyncs", phaseSyncs);
        body.put("actorSeatId", actorSeatId);
        body.put("actorPhaseSync", WsPushService.phasePayload(actorSeatId, gameEngine.buildPhaseSync(roomId, actorSeatId)));
        return body;
    }

    private static Map<String, Object> envelope(String type, Map<String, Object> payload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("payload", payload);
        return body;
    }
}

package com.werewolfengine.game.chat;

import com.werewolfengine.game.event.OutboundMessage;
import com.werewolfengine.game.lastwords.LastWordsFlow;
import com.werewolfengine.game.model.ActionAck;
import com.werewolfengine.game.model.ActionErrorCode;
import com.werewolfengine.game.model.ActionErrorCode;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.sync.PhaseSyncBuilder;
import com.werewolfengine.message.payload.ActionAckPayload;
import com.werewolfengine.message.payload.PhaseSyncPayload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PRD §4.6 — {@code CHAT_MESSAGE} (scope {@code WEREWOLF} / {@code ALL}).
 */
@Service
public class ChatMessageService {

    public ChatResult handle(GameRoomState room, int playerId, String scope, String content) {
        if (content == null || content.isBlank()) {
            return ChatResult.fail(ActionErrorCode.INVALID_ACTION, "content is required", room.getPhase());
        }
        PlayerState actor = room.getPlayer(playerId);
        if (actor == null || !actor.isAlive()) {
            return ChatResult.fail(ActionErrorCode.INVALID_ACTION, "Player not alive", room.getPhase());
        }
        return switch (scope) {
            case "WEREWOLF" -> handleWolf(room, actor, content.trim());
            case "ALL" -> handlePublic(room, actor, content.trim());
            default -> ChatResult.fail(ActionErrorCode.INVALID_ACTION, "Unknown scope: " + scope, room.getPhase());
        };
    }

    private ChatResult handleWolf(GameRoomState room, PlayerState actor, String content) {
        if (room.getPhase() != GamePhase.NIGHT_WOLF) {
            return ChatResult.fail(ActionErrorCode.INVALID_PHASE, "Wolf chat only in NIGHT_WOLF", room.getPhase());
        }
        if (actor.getRole() != Role.WEREWOLF) {
            return ChatResult.fail(ActionErrorCode.INVALID_ACTION, "Only wolves may use WEREWOLF scope", room.getPhase());
        }
        room.setWolfChatInPhase(true);
        OutboundMessage.enqueueChat(room, "WEREWOLF", actor.getPlayerId(), content);
        ActionAck ack = ActionAck.ok("狼队消息已发送", room.getPhase(), null);
        List<PhaseSyncPayload> syncs = new ArrayList<>();
        for (int wolfId : room.aliveWolfIds()) {
            syncs.add(PhaseSyncBuilder.forPlayer(room, wolfId));
        }
        return ChatResult.ok(toPayload(ack), syncs);
    }

    private ChatResult handlePublic(GameRoomState room, PlayerState actor, String content) {
        GamePhase phase = room.getPhase();
        if (phase != GamePhase.DAY_DISCUSS && phase != GamePhase.LAST_WORDS) {
            return ChatResult.fail(ActionErrorCode.INVALID_PHASE, "Public chat only in DAY_DISCUSS or LAST_WORDS", room.getPhase());
        }
        if (!isCurrentPublicSpeaker(room, actor.getPlayerId())) {
            return ChatResult.fail(ActionErrorCode.NOT_YOUR_TURN, "Not your turn to speak", room.getPhase());
        }
        OutboundMessage.enqueueChat(room, "ALL", actor.getPlayerId(), content);
        ActionAck ack = ActionAck.ok("发言已广播", room.getPhase(), null);
        return ChatResult.ok(toPayload(ack), List.of());
    }

    private static boolean isCurrentPublicSpeaker(GameRoomState room, int playerId) {
        if (room.getPhase() == GamePhase.DAY_DISCUSS) {
            List<Integer> order = room.getDiscussOrder();
            int idx = room.getDiscussIndex();
            return !order.isEmpty() && idx < order.size() && order.get(idx) == playerId;
        }
        if (room.getPhase() == GamePhase.LAST_WORDS) {
            return LastWordsFlow.isCurrentSpeaker(room, playerId);
        }
        return false;
    }

    public record ChatResult(boolean success, ActionAckPayload ack, List<PhaseSyncPayload> phaseSyncs) {

        static ChatResult ok(ActionAckPayload ack, List<PhaseSyncPayload> syncs) {
            return new ChatResult(true, ack, syncs);
        }

        static ChatResult fail(ActionErrorCode code, String message, GamePhase serverPhase) {
            return new ChatResult(false, toPayload(ActionAck.fail(code, message, serverPhase)), List.of());
        }
    }

    private static ActionAckPayload toPayload(ActionAck ack) {
        return new ActionAckPayload(
                ack.success(),
                ack.message(),
                ack.code(),
                ack.serverPhase(),
                ack.playerSubState()
        );
    }
}

package com.werewolfengine.game.event;

import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.GameWinner;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queued server → client push (GAME_EVENT / CHAT_BROADCAST / GAME_OVER).
 * Drained by gateway {@link com.werewolfengine.gateway.WsPushService}.
 */
public record OutboundMessage(
        OutboundKind kind,
        String eventType,
        Map<String, Object> data,
        OutboundAudience audience
) {

    public enum OutboundKind {
        GAME_EVENT,
        CHAT_BROADCAST,
        GAME_OVER
    }

    public static OutboundMessage gameEvent(String eventType, Map<String, Object> data, OutboundAudience audience) {
        return new OutboundMessage(OutboundKind.GAME_EVENT, eventType, Map.copyOf(data), audience);
    }

    public static OutboundMessage chatBroadcast(
            String scope,
            int playerId,
            String content,
            GamePhase phase,
            int round
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", scope);
        data.put("playerId", playerId);
        data.put("content", content);
        data.put("phase", phase.name());
        data.put("round", round);
        OutboundAudience audience = "WEREWOLF".equals(scope) ? OutboundAudience.WOLF_ONLY : OutboundAudience.ALIVE_CONNECTED;
        return new OutboundMessage(OutboundKind.CHAT_BROADCAST, null, data, audience);
    }

    public static OutboundMessage gameOver(GameRoomState room, GameWinner winner) {
        List<Map<String, Object>> players = new ArrayList<>();
        for (PlayerState p : room.getPlayers().values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", p.getPlayerId());
            row.put("role", p.getRole() != null ? p.getRole().name() : null);
            row.put("alive", p.isAlive());
            players.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("winner", winner.name());
        data.put("round", room.getRound());
        data.put("players", players);
        return new OutboundMessage(OutboundKind.GAME_OVER, "GAME_OVER", data, OutboundAudience.ALL_CONNECTED);
    }

    public static void enqueueNightDeaths(GameRoomState room) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seats", List.copyOf(room.getLastNightDeaths()));
        data.put("round", room.getRound());
        room.enqueueOutbound(gameEvent("NIGHT_DEATHS", data, OutboundAudience.PUBLIC));
    }

    public static void enqueueExileAnnounced(GameRoomState room, int seat) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seat", seat);
        data.put("round", room.getRound());
        room.enqueueOutbound(gameEvent("EXILE_ANNOUNCED", data, OutboundAudience.PUBLIC));
    }

    public static void enqueueIdiotRevealed(GameRoomState room, int seat) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("playerId", seat);
        data.put("role", Role.IDIOT.name());
        data.put("canVote", false);
        data.put("message", seat + "号玩家被投票出局，翻牌为愚者，不离场，失去投票权");
        room.enqueueOutbound(gameEvent("IDIOT_REVEALED", data, OutboundAudience.PUBLIC));
    }

    public static void enqueueHunterShot(GameRoomState room, int hunterSeat, Integer target) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seat", hunterSeat);
        if (target != null) {
            data.put("target", target);
        }
        data.put("skipped", target == null);
        data.put("round", room.getRound());
        room.enqueueOutbound(gameEvent("HUNTER_SHOT", data, OutboundAudience.PUBLIC));
    }

    public static void enqueueGameOver(GameRoomState room, GameWinner winner) {
        room.enqueueOutbound(gameOver(room, winner));
    }

    public static void enqueueChat(GameRoomState room, String scope, int playerId, String content) {
        room.enqueueOutbound(chatBroadcast(scope, playerId, content, room.getPhase(), room.getRound()));
    }
}

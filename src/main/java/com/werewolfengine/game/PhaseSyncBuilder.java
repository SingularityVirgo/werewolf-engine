package com.werewolfengine.game;

import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.message.payload.PhaseSyncPayload;

import java.util.ArrayList;
import java.util.List;

/** Builds per-seat PHASE_SYNC payloads for gateway (B). */
public final class PhaseSyncBuilder {

    private PhaseSyncBuilder() {
    }

    public static PhaseSyncPayload forPlayer(GameRoomState room, int viewerPlayerId) {
        PlayerState viewer = room.getPlayer(viewerPlayerId);
        Role yourRole = viewer != null ? viewer.getRole() : null;
        boolean canAct = canAct(room, viewer);
        Boolean wolfChat = room.getPhase() == GamePhase.NIGHT_WOLF ? room.isWolfChatInPhase() : null;

        return new PhaseSyncPayload(
                room.getPhase(),
                room.getRound(),
                defaultCountdown(room.getPhase()),
                room.alivePlayerIds(),
                yourRole,
                yourTeammates(room, viewer),
                canAct,
                canVote(viewer),
                null,
                wolfChat
        );
    }

    private static Integer defaultCountdown(GamePhase phase) {
        return switch (phase) {
            case NIGHT_WOLF -> 30;
            case ROLE_ASSIGN -> 5;
            case NIGHT_START, DAY_ANNOUNCE, VOTE_RESULT -> 5;
            default -> null;
        };
    }

    private static boolean canAct(GameRoomState room, PlayerState viewer) {
        if (viewer == null || !viewer.isAlive()) {
            return false;
        }
        return switch (room.getPhase()) {
            case NIGHT_WOLF -> viewer.getRole() == Role.WEREWOLF;
            default -> false;
        };
    }

    private static Boolean canVote(PlayerState viewer) {
        if (viewer == null || !viewer.isAlive()) {
            return false;
        }
        return true;
    }

    private static List<Integer> yourTeammates(GameRoomState room, PlayerState viewer) {
        if (viewer == null || viewer.getRole() != Role.WEREWOLF) {
            return null;
        }
        List<Integer> teammates = new ArrayList<>();
        for (int wolfId : room.aliveWolfIds()) {
            if (wolfId != viewer.getPlayerId()) {
                teammates.add(wolfId);
            }
        }
        return teammates;
    }
}

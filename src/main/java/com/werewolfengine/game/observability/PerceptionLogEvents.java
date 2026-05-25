package com.werewolfengine.game.observability;

import com.werewolfengine.game.event.OutboundMessage;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.GameWinner;

import java.util.stream.Collectors;

/** P0 system lines for seat perception (ADR-004 §4.2). */
public final class PerceptionLogEvents {

    private PerceptionLogEvents() {
    }

    public static void nightDeaths(ActionLogService log, GameRoomState room) {
        if (room == null) {
            return;
        }
        if (log != null) {
            String seats = room.getLastNightDeaths().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            log.recordSystemEvent(
                    room.getRoomId(),
                    room.getRound(),
                    GamePhase.NIGHT_DEATH_ANNOUNCE,
                    "NIGHT_DEATHS seats=" + seats,
                    null
            );
        }
        OutboundMessage.enqueueNightDeaths(room);
    }

    public static void exileAnnounced(ActionLogService log, GameRoomState room, int seat) {
        if (room == null) {
            return;
        }
        if (log != null) {
            log.recordSystemEvent(
                    room.getRoomId(),
                    room.getRound(),
                    GamePhase.EXILE_DEATH_ANNOUNCE,
                    "EXILE_ANNOUNCED seat=" + seat,
                    seat
            );
        }
        OutboundMessage.enqueueExileAnnounced(room, seat);
    }

    public static void idiotRevealed(ActionLogService log, GameRoomState room, int seat) {
        if (room == null) {
            return;
        }
        if (log != null) {
            log.recordSystemEvent(
                    room.getRoomId(),
                    room.getRound(),
                    room.getPhase(),
                    "IDIOT_REVEALED seat=" + seat,
                    seat
            );
        }
        OutboundMessage.enqueueIdiotRevealed(room, seat);
    }

    public static void gameOver(ActionLogService log, GameRoomState room, GameWinner winner) {
        if (room == null || winner == null) {
            return;
        }
        if (log != null) {
            log.recordSystemEvent(
                    room.getRoomId(),
                    room.getRound(),
                    GamePhase.GAME_OVER,
                    "GAME_OVER winner=" + winner.name(),
                    null
            );
        }
        OutboundMessage.enqueueGameOver(room, winner);
    }

    public static void hunterShot(ActionLogService log, GameRoomState room, int hunterSeat, Integer target) {
        if (room == null) {
            return;
        }
        if (log != null) {
            String message = target != null
                    ? "HUNTER_SHOT seat=" + hunterSeat + " target=" + target
                    : "HUNTER_SHOT seat=" + hunterSeat + " skipped";
            log.recordSystemEvent(
                    room.getRoomId(),
                    room.getRound(),
                    GamePhase.HUNTER_SHOOT,
                    message,
                    target
            );
        }
        OutboundMessage.enqueueHunterShot(room, hunterSeat, target);
    }
}

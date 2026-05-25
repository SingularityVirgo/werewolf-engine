package com.werewolfengine.room;

import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;

/**
 * Fills AI seats and validates lobby before {@code startGame} (PRD §4.2.3).
 */
final class RoomLobbyPreparer {

    private RoomLobbyPreparer() {
    }

    static void prepareForStart(GameRoomState room, RoomLobby lobby) {
        int humans = 0;
        int humansReady = 0;
        for (PlayerState p : room.getPlayers().values()) {
            if (p.getHumanUserId() != null) {
                humans++;
                if (p.isReady()) {
                    humansReady++;
                }
            }
        }
        if (humans == 0) {
            throw new IllegalStateException("At least one human player required to start");
        }
        if (humansReady != humans) {
            throw new IllegalStateException("All seated humans must be ready");
        }
        int aiSeats = RoomLobby.MAX_PLAYERS - humans;
        if (lobby.aiCount() > 0 && aiSeats != lobby.aiCount()) {
            throw new IllegalStateException(
                    "Expected " + lobby.aiCount() + " AI seats but lobby has " + humans
                            + " human(s) (" + aiSeats + " AI seats)");
        }
        for (PlayerState p : room.getPlayers().values()) {
            if (p.getHumanUserId() == null) {
                p.setReady(true);
            }
        }
    }

    static int findFirstJoinableSeat(GameRoomState room, RoomLobby lobby, Long userId) {
        for (int seat = 1; seat <= RoomLobby.MAX_PLAYERS; seat++) {
            if (lobby.isAiReservedSeat(seat)) {
                continue;
            }
            PlayerState p = room.getPlayer(seat);
            if (p == null) {
                continue;
            }
            Long occupant = p.getHumanUserId();
            if (occupant == null) {
                return seat;
            }
            if (userId != null && userId.equals(occupant)) {
                return seat;
            }
        }
        return -1;
    }
}

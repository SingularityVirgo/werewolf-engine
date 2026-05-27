package com.werewolfengine.room.persistence;

/**
 * Lobby MySQL write-through (ADR-007 phase 1). No-op when {@code werewolf.persistence.mysql-room=false}.
 */
public interface RoomLobbyPersistence {

    RoomLobbyPersistence NO_OP = new RoomLobbyPersistence() {
    };

    default void onRoomCreated(String roomId, Long hostUserId, int aiCount, String boardType) {
    }

    default void onPlayerJoined(String roomId, int seatId, Long userId) {
    }

    default void onPlayerReady(String roomId, int seatId, boolean ready) {
    }

    default void onPlayerLeft(String roomId, int seatId) {
    }

    default void onGameStarted(String roomId, com.werewolfengine.game.model.GameRoomState room) {
    }

    default void onRoomDissolved(String roomId) {
    }

    default void onGameEnded(String roomId) {
    }
}

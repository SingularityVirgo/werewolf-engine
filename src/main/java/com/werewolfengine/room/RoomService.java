package com.werewolfengine.room;

import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.game.model.GameRoomState;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class RoomService {

    private final GameEngineService gameEngine;
    private final Map<String, RoomSession> sessions = new ConcurrentHashMap<>();

    public RoomService(GameEngineService gameEngine) {
        this.gameEngine = gameEngine;
    }

    public RoomSnapshot createRoom(String roomId) {
        GameRoomState room = gameEngine.createRoom(roomId);
        sessions.put(room.getRoomId(), new RoomSession(room.getRoomId()));
        return snapshot(room.getRoomId());
    }

    public SeatSnapshot joinRoom(String roomId, int seatId, Long userId) {
        GameRoomState room = gameEngine.getRoomState(roomId);
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room not waiting: " + roomId);
        }
        PlayerState seat = room.getPlayer(seatId);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found: " + seatId);
        }
        if (seat.getHumanUserId() != null && !seat.getHumanUserId().equals(userId)) {
            throw new IllegalStateException("Seat already occupied: " + seatId);
        }
        seat.setHumanUserId(userId);
        seat.setReady(true);
        return new SeatSnapshot(roomId, seatId, userId, seat.isReady(), room.getPhase());
    }

    public SeatSnapshot setReady(String roomId, int seatId, boolean ready) {
        GameRoomState room = gameEngine.getRoomState(roomId);
        PlayerState seat = room.getPlayer(seatId);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found: " + seatId);
        }
        seat.setReady(ready);
        return new SeatSnapshot(roomId, seatId, seat.getHumanUserId(), seat.isReady(), room.getPhase());
    }

    public GameStateMachine.StartGameResult startRoom(String roomId) {
        return gameEngine.startGame(roomId);
    }

    public RoomSnapshot snapshot(String roomId) {
        GameRoomState room = gameEngine.getRoomState(roomId);
        return new RoomSnapshot(
                room.getRoomId(),
                room.getStatus().name(),
                room.getPhase().name(),
                room.getRound()
        );
    }

    public RoomSession session(String roomId) {
        return sessions.computeIfAbsent(roomId, RoomSession::new);
    }

    public record RoomSession(String roomId) {
    }

    public record RoomSnapshot(
            String roomId,
            String status,
            String phase,
            int round
    ) {
    }

    public record SeatSnapshot(
            String roomId,
            int seatId,
            Long userId,
            boolean ready,
            GamePhase phase
    ) {
    }
}

package com.werewolfengine.room;

import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.gateway.ConnectionManager;
import com.werewolfengine.gateway.RoomExecutionGuard;
import com.werewolfengine.gateway.RoomPhaseTickScheduler;
import com.werewolfengine.gateway.WsPushService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final GameEngineService gameEngine;
    private final RoomExecutionGuard roomGuard;
    private final WsPushService wsPushService;
    private final RoomPhaseTickScheduler phaseTickScheduler;
    private final ConnectionManager connectionManager;
    private final Map<String, RoomLobby> lobbies = new ConcurrentHashMap<>();

    public RoomService(
            GameEngineService gameEngine,
            RoomExecutionGuard roomGuard,
            WsPushService wsPushService,
            RoomPhaseTickScheduler phaseTickScheduler,
            ConnectionManager connectionManager
    ) {
        this.gameEngine = gameEngine;
        this.roomGuard = roomGuard;
        this.wsPushService = wsPushService;
        this.phaseTickScheduler = phaseTickScheduler;
        this.connectionManager = connectionManager;
    }

    public RoomSnapshot createRoom(String roomId, Long hostUserId, int aiCount) {
        GameRoomState room = gameEngine.createRoom(roomId);
        RoomLobby lobby = new RoomLobby(hostUserId, aiCount);
        lobbies.put(room.getRoomId(), lobby);
        return snapshot(room.getRoomId());
    }

    public SeatSnapshot joinRoom(String roomId, Integer seatId, Long userId) {
        return roomGuard.execute(roomId, () -> {
            GameRoomState room = gameEngine.getRoomState(roomId);
            requireWaiting(room);
            RoomLobby lobby = lobby(roomId);
            int resolvedSeat = seatId != null ? seatId : RoomLobbyPreparer.findFirstJoinableSeat(room, lobby, userId);
            if (resolvedSeat < 1) {
                throw new IllegalStateException("No joinable seat available");
            }
            if (lobby.isAiReservedSeat(resolvedSeat)) {
                throw new IllegalStateException("Seat reserved for AI: " + resolvedSeat);
            }
            PlayerState seat = room.getPlayer(resolvedSeat);
            if (seat == null) {
                throw new IllegalArgumentException("Seat not found: " + resolvedSeat);
            }
            if (seat.getHumanUserId() != null && userId != null && !seat.getHumanUserId().equals(userId)) {
                throw new IllegalStateException("Seat already occupied: " + resolvedSeat);
            }
            seat.setHumanUserId(userId);
            return new SeatSnapshot(roomId, resolvedSeat, userId, seat.isReady(), room.getPhase());
        });
    }

    public SeatSnapshot setReady(String roomId, int seatId, boolean ready) {
        return roomGuard.execute(roomId, () -> {
            GameRoomState room = gameEngine.getRoomState(roomId);
            PlayerState seat = room.getPlayer(seatId);
            if (seat == null) {
                throw new IllegalArgumentException("Seat not found: " + seatId);
            }
            if (ready && seat.getHumanUserId() == null && lobby(roomId).isAiReservedSeat(seatId)) {
                throw new IllegalStateException("AI-reserved seat cannot be readied by human: " + seatId);
            }
            seat.setReady(ready);
            return new SeatSnapshot(roomId, seatId, seat.getHumanUserId(), seat.isReady(), room.getPhase());
        });
    }

    public SeatSnapshot leaveRoom(String roomId, int seatId, Long userId) {
        return roomGuard.execute(roomId, () -> {
            GameRoomState room = gameEngine.getRoomState(roomId);
            requireWaiting(room);
            PlayerState seat = room.getPlayer(seatId);
            if (seat == null) {
                throw new IllegalArgumentException("Seat not found: " + seatId);
            }
            if (seat.getHumanUserId() != null && userId != null && !seat.getHumanUserId().equals(userId)) {
                throw new IllegalStateException("Not your seat: " + seatId);
            }
            seat.setHumanUserId(null);
            seat.setReady(false);
            connectionManager.findBySeat(roomId, seatId)
                    .ifPresent(rec -> connectionManager.remove(rec.session().getId()));
            return new SeatSnapshot(roomId, seatId, null, false, room.getPhase());
        });
    }

    public void dissolveRoom(String roomId, Long requesterUserId) {
        roomGuard.execute(roomId, () -> {
            GameRoomState room = gameEngine.getRoomState(roomId);
            requireWaiting(room);
            RoomLobby lobby = lobby(roomId);
            if (lobby.hostUserId() != null && requesterUserId != null
                    && !lobby.hostUserId().equals(requesterUserId)) {
                throw new IllegalStateException("Only host may dissolve room");
            }
            phaseTickScheduler.stop(roomId);
            connectionManager.removeRoom(roomId);
            gameEngine.removeRoom(roomId);
            lobbies.remove(roomId);
            return null;
        });
    }

    public GameStateMachine.StartGameResult startRoom(String roomId, Long requesterUserId) {
        GameStateMachine.StartGameResult result = roomGuard.execute(roomId, () -> {
            GameRoomState room = gameEngine.getRoomState(roomId);
            requireWaiting(room);
            RoomLobby lobby = lobby(roomId);
            if (lobby.hostUserId() != null && requesterUserId != null
                    && !lobby.hostUserId().equals(requesterUserId)) {
                throw new IllegalStateException("Only host may start game");
            }
            RoomLobbyPreparer.prepareForStart(room, lobby);
            return gameEngine.startGame(roomId);
        });
        if (result.success()) {
            wsPushService.pushPhaseSyncToConnected(roomId);
            wsPushService.flushOutbound(roomId);
            phaseTickScheduler.start(roomId);
        }
        return result;
    }

    public Map<String, Object> tickPhase(String roomId) {
        return phaseTickScheduler.tickOnceResponse(roomId);
    }

    public RoomSnapshot snapshot(String roomId) {
        GameRoomState room = gameEngine.getRoomState(roomId);
        RoomLobby lobby = lobby(roomId);
        List<SeatSnapshot> seats = new ArrayList<>();
        int readyCount = 0;
        int humanCount = 0;
        for (PlayerState p : room.getPlayers().values()) {
            boolean aiSeat = p.getHumanUserId() == null;
            if (!aiSeat) {
                humanCount++;
            }
            if (p.isReady()) {
                readyCount++;
            }
            seats.add(new SeatSnapshot(
                    room.getRoomId(),
                    p.getPlayerId(),
                    p.getHumanUserId(),
                    p.isReady(),
                    room.getPhase(),
                    aiSeat || lobby.isAiReservedSeat(p.getPlayerId())
            ));
        }
        return new RoomSnapshot(
                room.getRoomId(),
                room.getStatus().name(),
                room.getPhase().name(),
                room.getRound(),
                RoomLobby.MAX_PLAYERS,
                lobby.aiCount(),
                lobby.hostUserId(),
                readyCount,
                humanCount,
                seats
        );
    }

    RoomLobby lobby(String roomId) {
        return lobbies.computeIfAbsent(roomId, id -> new RoomLobby(null, 0));
    }

    private static void requireWaiting(GameRoomState room) {
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room not in WAITING: " + room.getRoomId());
        }
    }

    public record RoomSnapshot(
            String roomId,
            String status,
            String phase,
            int round,
            int maxPlayers,
            int aiCount,
            Long hostUserId,
            int readyCount,
            int humanCount,
            List<SeatSnapshot> seats
    ) {
    }

    public record SeatSnapshot(
            String roomId,
            int seatId,
            Long userId,
            boolean ready,
            GamePhase phase,
            boolean aiSeat
    ) {
        SeatSnapshot(String roomId, int seatId, Long userId, boolean ready, GamePhase phase) {
            this(roomId, seatId, userId, ready, phase, userId == null);
        }
    }
}

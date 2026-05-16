package com.werewolfengine.game;

import com.werewolfengine.game.model.ActionAck;
import com.werewolfengine.game.model.ActionErrorCode;
import com.werewolfengine.game.model.GameActionCommand;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.message.payload.ActionAckPayload;
import com.werewolfengine.message.payload.PhaseSyncPayload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative game state machine (Week1 Day1-2 skeleton).
 * Scope: WAITING → … → NIGHT_WOLF, collect KILL / WOLF_CHAT.
 */
@Component
public class GameStateMachine {

    private final ConcurrentHashMap<String, GameRoomState> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> roomLocks = new ConcurrentHashMap<>();

    public GameRoomState createRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, GameRoomState::new);
    }

    public Optional<GameRoomState> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    /**
     * Dev / B bridge: mark all seats ready (WAITING only).
     */
    public void markAllReady(String roomId) {
        withRoom(roomId, room -> {
            if (room.getStatus() != RoomStatus.WAITING) {
                throw new IllegalStateException("Room not in WAITING: " + roomId);
            }
            room.getPlayers().values().forEach(p -> p.setReady(true));
            return null;
        });
    }

    /**
     * WAITING → ROLE_ASSIGN → NIGHT_START → NIGHT_WOLF (instant assign for skeleton).
     */
    public StartGameResult startGame(String roomId) {
        return withRoom(roomId, room -> {
            if (room.getStatus() != RoomStatus.WAITING) {
                return StartGameResult.failure(ActionErrorCode.ROOM_NOT_WAITING,
                        "Room must be WAITING to start");
            }
            if (!allSeatsReady(room)) {
                return StartGameResult.failure(ActionErrorCode.INVALID_ACTION,
                        "All 12 players must be ready");
            }

            room.setStatus(RoomStatus.PLAYING);
            room.setPhase(GamePhase.ROLE_ASSIGN);
            RoleAssigner.assign(room);

            room.setRound(1);
            room.setPhase(GamePhase.NIGHT_START);
            enterNightWolf(room);

            List<PhaseSyncPayload> syncs = buildWolfPhaseSyncs(room);
            return StartGameResult.success(room.getPhase(), syncs);
        });
    }

    public HandleActionResult handleAction(String roomId, GameActionCommand command) {
        return withRoom(roomId, room -> {
            if (room.getStatus() != RoomStatus.PLAYING) {
                ActionAck ack = ActionAck.fail(ActionErrorCode.INVALID_PHASE,
                        "Room is not playing", room.getPhase());
                return HandleActionResult.of(ack, List.of());
            }

            if (command.clientPhase() != null && command.clientPhase() != room.getPhase()) {
                ActionAck ack = ActionAck.fail(ActionErrorCode.INVALID_PHASE,
                        "Phase mismatch", room.getPhase());
                return HandleActionResult.of(ack, List.of());
            }

            PlayerState actor = room.getPlayer(command.playerId());
            if (actor == null || !actor.isAlive()) {
                ActionAck ack = ActionAck.fail(ActionErrorCode.INVALID_ACTION,
                        "Player not alive or unknown", room.getPhase());
                return HandleActionResult.of(ack, List.of());
            }

            return switch (command.action()) {
                case KILL -> handleKill(room, actor, command.target());
                case WOLF_CHAT -> handleWolfChat(room, actor);
                default -> {
                    ActionAck ack = ActionAck.fail(ActionErrorCode.INVALID_ACTION,
                            "Action not supported in skeleton: " + command.action(), room.getPhase());
                    yield HandleActionResult.of(ack, List.of());
                }
            };
        });
    }

    public PhaseSyncPayload buildPhaseSync(String roomId, int viewerPlayerId) {
        return withRoom(roomId, room -> PhaseSyncBuilder.forPlayer(room, viewerPlayerId));
    }

    public ActionAckPayload toPayload(ActionAck ack) {
        return new ActionAckPayload(
                ack.success(),
                ack.message(),
                ack.code(),
                ack.serverPhase(),
                ack.playerSubState()
        );
    }

    private HandleActionResult handleKill(GameRoomState room, PlayerState actor, Integer targetId) {
        if (room.getPhase() != GamePhase.NIGHT_WOLF) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.INVALID_PHASE, "KILL only in NIGHT_WOLF", room.getPhase()),
                    List.of());
        }
        if (actor.getRole() != Role.WEREWOLF) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.INVALID_ACTION, "Only wolves can KILL", room.getPhase()),
                    List.of());
        }
        if (targetId == null) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.INVALID_TARGET, "KILL requires target", room.getPhase()),
                    List.of());
        }

        PlayerState target = room.getPlayer(targetId);
        if (target == null || !target.isAlive()) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.INVALID_TARGET, "Target must be alive", room.getPhase()),
                    List.of());
        }

        if (target.getRole() == Role.WEREWOLF && !room.isWolfChatInPhase()) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.WOLF_CHAT_REQUIRED,
                            "刀狼队友或自刀前，须在本夜晚狼人阶段先进行狼队频道商议",
                            room.getPhase()),
                    List.of());
        }

        room.getWolfKillVotes().put(actor.getPlayerId(), targetId);
        ActionAck ack = ActionAck.ok("刀人目标已记录", room.getPhase(), "WAITING_WOLF_CONSENSUS");
        return HandleActionResult.of(ack, List.of());
    }

    private HandleActionResult handleWolfChat(GameRoomState room, PlayerState actor) {
        if (room.getPhase() != GamePhase.NIGHT_WOLF) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.INVALID_PHASE, "WOLF_CHAT only in NIGHT_WOLF", room.getPhase()),
                    List.of());
        }
        if (actor.getRole() != Role.WEREWOLF) {
            return HandleActionResult.of(
                    ActionAck.fail(ActionErrorCode.INVALID_ACTION, "Only wolves can WOLF_CHAT", room.getPhase()),
                    List.of());
        }

        room.setWolfChatInPhase(true);
        ActionAck ack = ActionAck.ok("狼队商议已记录", room.getPhase(), null);
        return HandleActionResult.of(ack, buildWolfPhaseSyncs(room));
    }

    private void enterNightWolf(GameRoomState room) {
        room.setPhase(GamePhase.NIGHT_WOLF);
        room.resetWolfNightState();
    }

    private List<PhaseSyncPayload> buildWolfPhaseSyncs(GameRoomState room) {
        List<PhaseSyncPayload> syncs = new ArrayList<>();
        for (int wolfId : room.aliveWolfIds()) {
            syncs.add(PhaseSyncBuilder.forPlayer(room, wolfId));
        }
        return syncs;
    }

    private static boolean allSeatsReady(GameRoomState room) {
        return room.getPlayers().values().stream().allMatch(PlayerState::isReady);
    }

    private <T> T withRoom(String roomId, RoomOperation<T> operation) {
        Object lock = roomLocks.computeIfAbsent(roomId, id -> new Object());
        synchronized (lock) {
            GameRoomState room = rooms.computeIfAbsent(roomId, GameRoomState::new);
            return operation.apply(room);
        }
    }

    @FunctionalInterface
    private interface RoomOperation<T> {
        T apply(GameRoomState room);
    }

    public record StartGameResult(
            boolean success,
            ActionErrorCode errorCode,
            String message,
            GamePhase phase,
            List<PhaseSyncPayload> phaseSyncs
    ) {
        static StartGameResult success(GamePhase phase, List<PhaseSyncPayload> syncs) {
            return new StartGameResult(true, null, null, phase, syncs);
        }

        static StartGameResult failure(ActionErrorCode code, String message) {
            return new StartGameResult(false, code, message, null, List.of());
        }
    }

    public record HandleActionResult(ActionAck ack, List<PhaseSyncPayload> phaseSyncs) {
        static HandleActionResult of(ActionAck ack, List<PhaseSyncPayload> syncs) {
            return new HandleActionResult(ack, syncs);
        }
    }
}

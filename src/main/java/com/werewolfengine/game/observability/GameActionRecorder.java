package com.werewolfengine.game.observability;

import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GameActionCommand;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import org.springframework.stereotype.Component;

/**
 * Single write path for player {@link ActionLogEntry} lines (G-08).
 */
@Component
public class GameActionRecorder {

    private final GameStateMachine stateMachine;
    private final ActionLogService actionLog;

    public GameActionRecorder(GameStateMachine stateMachine, ActionLogService actionLog) {
        this.stateMachine = stateMachine;
        this.actionLog = actionLog;
    }

    public GameStateMachine.HandleActionResult recordAndHandle(
            String roomId,
            GameRoomState roomBefore,
            GameActionCommand command,
            String modelId
    ) {
        int round = roomBefore.getRound();
        GamePhase phase = command.clientPhase() != null ? command.clientPhase() : roomBefore.getPhase();
        Integer effectiveTarget = effectiveTarget(roomBefore, command);
        GameStateMachine.HandleActionResult result = stateMachine.handleAction(roomId, command);
        GameRoomState after = stateMachine.getRoom(roomId).orElse(roomBefore);
        actionLog.recordPlayerAction(
                roomId, round, phase, after, command, result.ack(), effectiveTarget, modelId);
        return result;
    }

    public GameStateMachine.HandleActionResult recordAdvanceDayAnnounce(String roomId) {
        GameRoomState room = stateMachine.getRoom(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        GamePhase phaseBefore = room.getPhase();
        int round = room.getRound();
        GameStateMachine.HandleActionResult result = stateMachine.advanceDayAnnounce(roomId);
        actionLog.recordSystemEvent(roomId, round, phaseBefore, "advanceDayAnnounce", null);
        return result;
    }

    private static Integer effectiveTarget(GameRoomState room, GameActionCommand command) {
        if (command.action() == GameActionType.SAVE) {
            return room.getPendingWolfKillTarget();
        }
        return command.target();
    }
}

package com.werewolfengine.game.orchestration;

import com.werewolfengine.ai.api.AIService;
import com.werewolfengine.ai.api.DecisionResult;
import com.werewolfengine.ai.api.PlayerIntent;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GameActionCommand;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.sync.PhaseCountdown;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Applies PRD §4.3.3 timeout fallbacks when {@link PhaseCountdown} expires (ADR-005 P-05).
 */
@Component
public class PhaseTimeoutHandler {

    private final GameStateMachine stateMachine;
    private final TurnActorResolver turnResolver;
    private final AIService aiService;
    private final ActionLogService actionLog;

    public PhaseTimeoutHandler(
            GameStateMachine stateMachine,
            TurnActorResolver turnResolver,
            AIService aiService,
            ActionLogService actionLog
    ) {
        this.stateMachine = stateMachine;
        this.turnResolver = turnResolver;
        this.aiService = aiService;
        this.actionLog = actionLog;
    }

    /**
     * @return true if room state may have changed
     */
    public boolean applyIfExpired(String roomId, GameRoomState room) {
        if (!PhaseCountdown.isEnabled() || !PhaseCountdown.isExpired(room)) {
            return false;
        }
        GamePhase phase = room.getPhase();
        return switch (phase) {
            case NIGHT_DEATH_ANNOUNCE, EXILE_DEATH_ANNOUNCE -> advanceAnnounce(roomId, room);
            case NIGHT_WOLF -> applyWolfPhaseTimeout(roomId, room);
            case NIGHT_SEER, NIGHT_WITCH -> applyNightRoleTimeout(roomId, room);
            case DAY_VOTE -> applyDayVoteTimeout(roomId, room);
            case DAY_DISCUSS, HUNTER_SHOOT, LAST_WORDS -> applyTurnTimeout(roomId, room);
            default -> false;
        };
    }

    private boolean advanceAnnounce(String roomId, GameRoomState room) {
        GamePhase before = room.getPhase();
        int round = room.getRound();
        stateMachine.advanceDayAnnounce(roomId);
        if (actionLog != null) {
            actionLog.recordSystemEvent(roomId, round, before, "phaseTimeout:advanceDayAnnounce", null);
        }
        return true;
    }

    private boolean applyWolfPhaseTimeout(String roomId, GameRoomState room) {
        if (stateMachine.applyTimedWolfPhaseFallback(roomId)) {
            logSystem(roomId, room, "phaseTimeout:WOLF_KILL_RESOLVED");
            return true;
        }
        return false;
    }

    private boolean applyNightRoleTimeout(String roomId, GameRoomState room) {
        if (stateMachine.applyTimedNightFallback(roomId)) {
            logSystem(roomId, room, "phaseTimeout:nightNoActor");
            return true;
        }
        return applyTurnTimeout(roomId, room);
    }

    private boolean applyDayVoteTimeout(String roomId, GameRoomState room) {
        if (stateMachine.applyTimedDayVoteFallback(roomId)) {
            logSystem(roomId, room, "phaseTimeout:DAY_VOTE_abstain_all");
            return true;
        }
        return false;
    }

    private boolean applyTurnTimeout(String roomId, GameRoomState room) {
        Optional<Integer> actor = turnResolver.nextActor(room);
        if (actor.isEmpty()) {
            return false;
        }
        int seat = actor.get();
        Optional<DecisionResult> decision = aiService.decideWithSource(room, seat);
        if (decision.isEmpty()) {
            return false;
        }
        PlayerIntent in = decision.get().intent();
        GameActionCommand cmd = new GameActionCommand(
                seat,
                in.action(),
                in.target(),
                room.getPhase(),
                in.content()
        );
        var result = stateMachine.handleAction(roomId, cmd);
        if (actionLog != null) {
            GameRoomState after = stateMachine.getRoom(roomId).orElse(room);
            actionLog.recordPlayerAction(
                    roomId,
                    room.getRound(),
                    room.getPhase(),
                    after,
                    cmd,
                    result.ack(),
                    cmd.target(),
                    decision.get().modelId()
            );
        }
        logSystem(roomId, room, "phaseTimeout:" + in.action(), in.target());
        return true;
    }

    private void logSystem(String roomId, GameRoomState room, String event) {
        logSystem(roomId, room, event, null);
    }

    private void logSystem(String roomId, GameRoomState room, String event, Integer target) {
        if (actionLog != null) {
            actionLog.recordSystemEvent(
                    roomId,
                    room.getRound(),
                    room.getPhase(),
                    event,
                    target
            );
        }
    }
}

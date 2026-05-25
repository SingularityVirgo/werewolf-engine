package com.werewolfengine.game.orchestration;

import com.werewolfengine.ai.api.PlayerIntent;
import com.werewolfengine.ai.policy.MockAIPlayer;
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
    private final MockAIPlayer mockAI;
    private final ActionLogService actionLog;

    public PhaseTimeoutHandler(
            GameStateMachine stateMachine,
            TurnActorResolver turnResolver,
            MockAIPlayer mockAI,
            ActionLogService actionLog
    ) {
        this.stateMachine = stateMachine;
        this.turnResolver = turnResolver;
        this.mockAI = mockAI;
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
            case NIGHT_DEATH_ANNOUNCE, EXILE_DEATH_ANNOUNCE -> {
                GamePhase before = room.getPhase();
                int round = room.getRound();
                stateMachine.advanceDayAnnounce(roomId);
                if (actionLog != null) {
                    actionLog.recordSystemEvent(roomId, round, before, "phaseTimeout:advanceDayAnnounce", null);
                }
                yield true;
            }
            case NIGHT_WOLF, NIGHT_SEER, NIGHT_WITCH, DAY_DISCUSS, DAY_VOTE, HUNTER_SHOOT, LAST_WORDS ->
                    applyPlayerPhaseTimeout(roomId, room);
            default -> false;
        };
    }

    private boolean applyPlayerPhaseTimeout(String roomId, GameRoomState room) {
        if (stateMachine.applyTimedNightFallback(roomId)) {
            return true;
        }
        Optional<Integer> actor = turnResolver.nextActor(room);
        if (actor.isEmpty()) {
            return false;
        }
        int seat = actor.get();
        Optional<PlayerIntent> intent = mockAI.decide(room, seat);
        if (intent.isEmpty()) {
            return false;
        }
        PlayerIntent in = intent.get();
        GameActionCommand cmd = new GameActionCommand(
                seat,
                in.action(),
                in.target(),
                room.getPhase(),
                in.content()
        );
        stateMachine.handleAction(roomId, cmd);
        if (actionLog != null) {
            actionLog.recordSystemEvent(
                    roomId,
                    room.getRound(),
                    room.getPhase(),
                    "phaseTimeout:" + in.action(),
                    in.target()
            );
        }
        return true;
    }
}

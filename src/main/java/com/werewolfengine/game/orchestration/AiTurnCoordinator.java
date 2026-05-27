package com.werewolfengine.game.orchestration;

import com.werewolfengine.ai.api.AIService;
import com.werewolfengine.ai.api.DecisionResult;
import com.werewolfengine.ai.api.PlayerIntent;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GameActionCommand;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.observability.GameActionRecorder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Single-step AI turn orchestration for gateway ticks and dev runners (ADR-003 §4).
 */
@Component
public class AiTurnCoordinator {

    private final GameStateMachine stateMachine;
    private final TurnActorResolver actorResolver;
    private final AIService aiService;
    private final GameActionRecorder actionRecorder;

    public AiTurnCoordinator(
            GameStateMachine stateMachine,
            TurnActorResolver actorResolver,
            AIService aiService,
            GameActionRecorder actionRecorder
    ) {
        this.stateMachine = stateMachine;
        this.actorResolver = actorResolver;
        this.aiService = aiService;
        this.actionRecorder = actionRecorder;
    }

    /**
     * Advances one logical step: system announce, or one AI {@link GameStateMachine#handleAction}.
     *
     * @return true if state changed; false if stuck (no actor / no intent / human seat)
     */
    public boolean tickOneStep(String roomId, GameRoomState room) {
        return switch (room.getPhase()) {
            case NIGHT_DEATH_ANNOUNCE, EXILE_DEATH_ANNOUNCE -> {
                actionRecorder.recordAdvanceDayAnnounce(roomId);
                yield true;
            }
            default -> submitNextAiIntent(roomId, room);
        };
    }

    private boolean submitNextAiIntent(String roomId, GameRoomState room) {
        Optional<Integer> actorId = actorResolver.nextAiActor(room);
        if (actorId.isEmpty()) {
            return false;
        }
        int playerId = actorId.get();
        GameRoomState fresh = stateMachine.getRoom(roomId).orElse(room);
        Optional<DecisionResult> decision = aiService.decideWithSource(fresh, playerId);
        if (decision.isEmpty()) {
            return false;
        }
        PlayerIntent in = decision.get().intent();
        GamePhase phase = fresh.getPhase();
        GameActionCommand cmd = new GameActionCommand(
                playerId,
                in.action(),
                in.target(),
                phase,
                in.content()
        );
        actionRecorder.recordAndHandle(roomId, fresh, cmd, decision.get().modelId());
        return true;
    }
}

package com.werewolfengine.game.testsupport;

import com.werewolfengine.ai.api.AIService;
import com.werewolfengine.ai.config.AiProperties;
import com.werewolfengine.ai.guard.AiLegalActions;
import com.werewolfengine.ai.parse.AiIntentParser;
import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.ai.memory.MemoryPromptFormatter;
import com.werewolfengine.ai.prompt.AiPromptBuilder;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.orchestration.AiTurnCoordinator;
import com.werewolfengine.game.orchestration.GamePhaseScheduler;
import com.werewolfengine.game.orchestration.MockGameRunner;
import com.werewolfengine.game.orchestration.PhaseTimeoutHandler;
import com.werewolfengine.game.orchestration.TurnActorResolver;
import com.werewolfengine.game.sync.PhaseCountdown;

/**
 * Wires {@link AiTurnCoordinator} + {@link MockGameRunner} for unit tests without Spring context.
 */
public final class GameTestAiSupport {

    private GameTestAiSupport() {
    }

    public record Harness(
            AIService aiService,
            AiTurnCoordinator turnCoordinator,
            MockGameRunner mockGameRunner,
            GamePhaseScheduler phaseScheduler
    ) {
    }

    public static Harness mockOnly(GameStateMachine stateMachine, ActionLogService actionLog) {
        PhaseCountdown.setEnabled(false);
        AIService ai = disabledAiService();
        TurnActorResolver resolver = new TurnActorResolver();
        AiTurnCoordinator coordinator = new AiTurnCoordinator(stateMachine, resolver, ai, actionLog);
        PhaseTimeoutHandler timeoutHandler = new PhaseTimeoutHandler(
                stateMachine,
                resolver,
                ai,
                actionLog
        );
        GamePhaseScheduler scheduler = new GamePhaseScheduler(
                stateMachine,
                coordinator,
                timeoutHandler,
                actionLog
        );
        MockGameRunner runner = new MockGameRunner(stateMachine, scheduler);
        return new Harness(ai, coordinator, runner, scheduler);
    }

    public static AIService disabledAiService() {
        AiProperties props = new AiProperties();
        props.setEnabled(false);
        return new AIService(
                props,
                new MockAIPlayer(),
                new AiPromptBuilder(),
                new MemoryPromptFormatter(),
                new AiIntentParser(),
                new AiLegalActions(),
                null,
                null
        );
    }
}

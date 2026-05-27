package com.werewolfengine.gateway;

import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.persistence.GameOverLifecycleService;
import com.werewolfengine.game.orchestration.GamePhaseScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomPhaseTickSchedulerTest {

    @Mock
    private GameEngineService gameEngine;

    @Mock
    private WsPushService wsPushService;

    @Mock
    private GameOverLifecycleService gameOverLifecycle;

    private RoomExecutionGuard roomGuard;
    private RoomPhaseTickScheduler scheduler;

    @BeforeEach
    void setUp() {
        roomGuard = new RoomExecutionGuard();
        scheduler = new RoomPhaseTickScheduler(gameEngine, roomGuard, wsPushService, gameOverLifecycle);
    }

    @Test
    void shouldPushAfterTick_advancedAndAiStepAndGameOver() {
        assertThat(RoomPhaseTickScheduler.shouldPushAfterTick(
                GamePhaseScheduler.TickResult.advanced("NIGHT_DEATH_ANNOUNCE"))).isTrue();
        assertThat(RoomPhaseTickScheduler.shouldPushAfterTick(
                GamePhaseScheduler.TickResult.aiStep("NIGHT_WOLF"))).isTrue();
        assertThat(RoomPhaseTickScheduler.shouldPushAfterTick(
                GamePhaseScheduler.TickResult.gameOver("GOOD"))).isTrue();
        assertThat(RoomPhaseTickScheduler.shouldPushAfterTick(
                GamePhaseScheduler.TickResult.noOp("ROLE_ASSIGN"))).isFalse();
        assertThat(RoomPhaseTickScheduler.shouldPushAfterTick(
                GamePhaseScheduler.TickResult.stuck("DAY_VOTE"))).isFalse();
        assertThat(RoomPhaseTickScheduler.shouldPushAfterTick(
                GamePhaseScheduler.TickResult.countdown("NIGHT_WOLF"))).isTrue();
    }

    @Test
    void tickOnce_pushesOnAiStep() {
        when(gameEngine.tickPhase("r1"))
                .thenReturn(GamePhaseScheduler.TickResult.aiStep("NIGHT_WOLF"));

        GamePhaseScheduler.TickResult result = scheduler.tickOnce("r1");

        assertThat(result.status()).isEqualTo("AI_STEP");
        verify(wsPushService).pushPhaseSyncToConnected("r1");
    }

    @Test
    void tickOnce_doesNotPushOnNoOp() {
        when(gameEngine.tickPhase("r1"))
                .thenReturn(GamePhaseScheduler.TickResult.noOp("ROLE_ASSIGN"));

        scheduler.tickOnce("r1");

        verify(wsPushService, never()).pushPhaseSyncToConnected(eq("r1"));
    }

    @Test
    void tickOnce_pushesOnGameOver() {
        when(gameEngine.tickPhase("r1"))
                .thenReturn(GamePhaseScheduler.TickResult.gameOver("WOLF"));

        GamePhaseScheduler.TickResult result = scheduler.tickOnce("r1");

        assertThat(result.status()).isEqualTo("GAME_OVER");
        verify(wsPushService).pushPhaseSyncToConnected("r1");
        verify(gameOverLifecycle).finalizeIfGameOver("r1");
    }
}

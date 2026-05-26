package com.werewolfengine.game.orchestration;

import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.sync.PhaseCountdown;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamePhaseSchedulerCountdownTest {

    @Mock
    private GameStateMachine stateMachine;

    @Mock
    private AiTurnCoordinator turnCoordinator;

    @Mock
    private PhaseTimeoutHandler phaseTimeoutHandler;

    @Mock
    private ActionLogService actionLog;

    private GamePhaseScheduler scheduler;

    @BeforeEach
    void setUp() {
        PhaseCountdown.setEnabled(true);
        scheduler = new GamePhaseScheduler(stateMachine, turnCoordinator, phaseTimeoutHandler, actionLog);
    }

    @AfterEach
    void restore() {
        PhaseCountdown.setEnabled(true);
    }

    @Test
    void tick_returnsCountdownWhilePhaseTimerActive() {
        GameRoomState room = new GameRoomState("r1");
        room.setPhase(GamePhase.NIGHT_WOLF);
        when(stateMachine.getRoom("r1")).thenReturn(Optional.of(room));

        GamePhaseScheduler.TickResult result = scheduler.tick("r1");

        assertThat(result.status()).isEqualTo("COUNTDOWN");
        verify(turnCoordinator).tickOneStep(eq("r1"), any());
        verify(phaseTimeoutHandler, never()).applyIfExpired(eq("r1"), any());
    }

    @Test
    void tick_returnsAiStepWhenAiActsDuringCountdown() {
        GameRoomState room = new GameRoomState("r1");
        room.setPhase(GamePhase.DAY_DISCUSS);
        when(stateMachine.getRoom("r1")).thenReturn(Optional.of(room));
        when(turnCoordinator.tickOneStep("r1", room)).thenReturn(true);

        GamePhaseScheduler.TickResult result = scheduler.tick("r1");

        assertThat(result.status()).isEqualTo("AI_STEP");
        verify(turnCoordinator).tickOneStep("r1", room);
        verify(phaseTimeoutHandler, never()).applyIfExpired(eq("r1"), any());
    }

    @Test
    void tick_appliesTimeoutWhenExpired() {
        GameRoomState room = new GameRoomState("r1");
        room.setPhase(GamePhase.NIGHT_WOLF);
        room.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1000);
        when(stateMachine.getRoom("r1")).thenReturn(Optional.of(room));
        when(phaseTimeoutHandler.applyIfExpired("r1", room)).thenReturn(true);

        GamePhaseScheduler.TickResult result = scheduler.tick("r1");

        assertThat(result.status()).isIn("ADVANCED", "AI_STEP");
        verify(phaseTimeoutHandler).applyIfExpired("r1", room);
        verify(turnCoordinator, never()).tickOneStep(eq("r1"), any());
    }
}

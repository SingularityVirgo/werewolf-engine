package com.werewolfengine.game;

import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.orchestration.GamePhaseScheduler;
import com.werewolfengine.game.testsupport.GameTestAiSupport;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.sync.PhaseCountdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GamePhaseSchedulerTest {

    @BeforeEach
    void disableCountdownForFastTicks() {
        PhaseCountdown.setEnabled(false);
    }

    @Test
    void tickAdvancesAnnouncePhase() {
        GameStateMachine sm = new GameStateMachine();
        ActionLogService log = new ActionLogService();
        GameTestAiSupport.Harness h = GameTestAiSupport.mockOnly(sm, log);
        GamePhaseScheduler scheduler = h.phaseScheduler();

        String roomId = "sched_announce";
        sm.createRoom(roomId);
        sm.markAllReady(roomId);
        sm.startGame(roomId);

        var result = scheduler.tick(roomId);
        assertThat(result.status()).isIn("AI_STEP", "ADVANCED");
    }

    @Test
    void tickEventuallyReachesGameOver() {
        GameStateMachine sm = new GameStateMachine();
        ActionLogService log = new ActionLogService();
        GameTestAiSupport.Harness h = GameTestAiSupport.mockOnly(sm, log);
        GamePhaseScheduler scheduler = h.phaseScheduler();

        String roomId = "sched_full";
        sm.createRoom(roomId);
        sm.markAllReady(roomId);
        sm.startGame(roomId);

        for (int i = 0; i < 20_000; i++) {
            var tick = scheduler.tick(roomId);
            if ("COUNTDOWN".equals(tick.status())) {
                sm.getRoom(roomId).ifPresent(r -> r.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1));
                continue;
            }
            if ("STUCK".equals(tick.status()) && sm.applyTimedNightFallback(roomId)) {
                continue;
            }
            if ("GAME_OVER".equals(tick.status())) {
                assertThat(sm.getRoom(roomId).orElseThrow().getPhase()).isEqualTo(GamePhase.GAME_OVER);
                return;
            }
        }
        throw new AssertionError("did not finish in 20000 ticks");
    }
}

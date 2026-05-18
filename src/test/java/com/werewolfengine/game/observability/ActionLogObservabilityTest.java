package com.werewolfengine.game.observability;

import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.orchestration.MockGameRunner;
import com.werewolfengine.game.testsupport.GameTestAiSupport;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * action_log phase/action must match submission-time phase (not post-transition room state).
 */
class ActionLogObservabilityTest {

    private final ActionLogService actionLog = new ActionLogService();
    private final GameStateMachine stateMachine = new GameStateMachine(actionLog);
    private final GameTestAiSupport.Harness harness = GameTestAiSupport.mockOnly(stateMachine, actionLog);
    private final MockGameRunner runner = harness.mockGameRunner();

    @RepeatedTest(3)
    void mockFullGameLogHasConsistentPhaseAndAction() {
        String roomId = "log_obs_" + System.nanoTime();
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        assertThat(stateMachine.startGame(roomId).success()).isTrue();

        assertThat(runner.runUntilGameOver(roomId).isFinished()).isTrue();

        List<ActionLogEntry> entries = actionLog.getLog(roomId);
        assertThat(entries).isNotEmpty();
        assertThat(entries.stream().anyMatch(e -> e.content() != null && e.content().startsWith("WOLF_KILL_RESOLVED")))
                .isTrue();
        assertThat(entries.stream().anyMatch(e -> e.content() != null && e.content().contains("advanceDayAnnounce")))
                .isTrue();
        assertThat(entries.stream().anyMatch(e -> e.content() != null && e.content().startsWith("NIGHT_DEATHS")))
                .isTrue();
        assertThat(entries.stream().anyMatch(e -> e.content() != null && e.content().startsWith("GAME_OVER winner=")))
                .isTrue();

        int mismatches = 0;
        for (ActionLogEntry e : entries) {
            if (e.playerId() == 0) {
                continue;
            }
            if (e.action() != null && !isLegalInPhase(e.phase(), e.action())) {
                mismatches++;
            }
        }
        assertThat(mismatches)
                .as("phase/action mismatches: %s", entries.stream().filter(e -> e.playerId() != 0 && e.action() != null
                        && !isLegalInPhase(e.phase(), e.action())).limit(5).toList())
                .isZero();
    }

    @Test
    void witchSaveLogsEffectiveKillTarget() {
        String roomId = "log_save";
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        stateMachine.startGame(roomId);

        runner.runUntilGameOver(roomId);

        boolean sawSaveWithTarget = actionLog.getLog(roomId).stream()
                .anyMatch(e -> e.action() == GameActionType.SAVE
                        && e.target() != null
                        && e.phase() == GamePhase.NIGHT_WITCH);
        assertThat(sawSaveWithTarget).isTrue();
    }

    private static boolean isLegalInPhase(GamePhase phase, GameActionType action) {
        Set<GameActionType> allowed = ALLOWED.get(phase);
        return allowed != null && allowed.contains(action);
    }

    private static final java.util.Map<GamePhase, Set<GameActionType>> ALLOWED = java.util.Map.ofEntries(
            java.util.Map.entry(GamePhase.NIGHT_WOLF, EnumSet.of(GameActionType.KILL, GameActionType.WOLF_CHAT)),
            java.util.Map.entry(GamePhase.NIGHT_SEER, EnumSet.of(GameActionType.CHECK, GameActionType.SKIP)),
            java.util.Map.entry(GamePhase.NIGHT_WITCH,
                    EnumSet.of(GameActionType.SAVE, GameActionType.POISON, GameActionType.SKIP)),
            java.util.Map.entry(GamePhase.DAY_DISCUSS, EnumSet.of(GameActionType.SPEAK, GameActionType.SKIP_SPEAK)),
            java.util.Map.entry(GamePhase.DAY_VOTE, EnumSet.of(GameActionType.VOTE, GameActionType.SKIP_VOTE)),
            java.util.Map.entry(GamePhase.HUNTER_SHOOT, EnumSet.of(GameActionType.SHOOT, GameActionType.SKIP)),
            java.util.Map.entry(GamePhase.LAST_WORDS, EnumSet.of(GameActionType.SPEAK, GameActionType.SKIP_SPEAK))
    );
}

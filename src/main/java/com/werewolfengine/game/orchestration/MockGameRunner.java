package com.werewolfengine.game.orchestration;

import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.GameWinner;
import org.springframework.stereotype.Component;

/**
 * Dev / load-test driver: runs {@link GamePhaseScheduler} until {@link GamePhase#GAME_OVER} (ADR-003).
 * When a phase countdown is active, advances the deadline so mock runs do not wall-clock wait.
 */
@Component
public class MockGameRunner {

    public static final int DEFAULT_MAX_STEPS = 8_000;

    private final GameStateMachine stateMachine;
    private final GamePhaseScheduler phaseScheduler;

    public MockGameRunner(GameStateMachine stateMachine, GamePhaseScheduler phaseScheduler) {
        this.stateMachine = stateMachine;
        this.phaseScheduler = phaseScheduler;
    }

    public RunResult runUntilGameOver(String roomId) {
        return runUntilGameOver(roomId, DEFAULT_MAX_STEPS);
    }

    public RunResult runUntilGameOver(String roomId, int maxSteps) {
        int steps = 0;
        GamePhase lastPhase = null;
        while (steps < maxSteps) {
            GameRoomState room = stateMachine.getRoom(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
            if (room.getPhase() == GamePhase.GAME_OVER) {
                return RunResult.finished(steps, room.getWinner());
            }
            GamePhaseScheduler.TickResult tick = phaseScheduler.tick(roomId);
            if ("COUNTDOWN".equals(tick.status())) {
                stateMachine.getRoom(roomId).ifPresent(r -> r.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1));
                continue;
            }
            if ("STUCK".equals(tick.status())) {
                if (applyStuckFallback(roomId)) {
                    steps++;
                    continue;
                }
                return RunResult.stuck(steps, room.getPhase(), lastPhase);
            }
            if ("NO_OP".equals(tick.status())) {
                return RunResult.stuck(steps, room.getPhase(), lastPhase);
            }
            if ("GAME_OVER".equals(tick.status())) {
                GameRoomState end = stateMachine.getRoom(roomId).orElseThrow();
                return RunResult.finished(steps + 1, end.getWinner());
            }
            lastPhase = stateMachine.getRoom(roomId).map(GameRoomState::getPhase).orElse(room.getPhase());
            steps++;
        }
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        return RunResult.maxStepsExceeded(steps, room.getPhase());
    }

    /** @deprecated use {@link GamePhaseScheduler#tick} */
    public boolean advanceOneStepPublic(String roomId) {
        GamePhaseScheduler.TickResult tick = phaseScheduler.tick(roomId);
        if ("COUNTDOWN".equals(tick.status())) {
            stateMachine.getRoom(roomId).ifPresent(r -> r.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1));
            tick = phaseScheduler.tick(roomId);
        }
        if ("STUCK".equals(tick.status()) && applyStuckFallback(roomId)) {
            tick = phaseScheduler.tick(roomId);
        }
        return !"STUCK".equals(tick.status()) && !"NO_OP".equals(tick.status());
    }

    /**
     * SM-level fallbacks when AI coordinator cannot advance (PRD §4.3.3 / G-06).
     */
    private boolean applyStuckFallback(String roomId) {
        if (stateMachine.applyTimedWolfPhaseFallback(roomId)) {
            return true;
        }
        if (stateMachine.applyTimedDayVoteFallback(roomId)) {
            return true;
        }
        return stateMachine.applyTimedNightFallback(roomId);
    }

    public record RunResult(
            Outcome outcome,
            int steps,
            GameWinner winner,
            GamePhase phase,
            GamePhase previousPhase
    ) {
        public enum Outcome {
            FINISHED,
            STUCK,
            MAX_STEPS
        }

        static RunResult finished(int steps, GameWinner winner) {
            return new RunResult(Outcome.FINISHED, steps, winner, GamePhase.GAME_OVER, null);
        }

        static RunResult stuck(int steps, GamePhase phase, GamePhase previousPhase) {
            return new RunResult(Outcome.STUCK, steps, null, phase, previousPhase);
        }

        static RunResult maxStepsExceeded(int steps, GamePhase phase) {
            return new RunResult(Outcome.MAX_STEPS, steps, null, phase, null);
        }

        public boolean isFinished() {
            return outcome == Outcome.FINISHED && winner != null;
        }
    }
}

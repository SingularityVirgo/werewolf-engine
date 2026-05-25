package com.werewolfengine.game.sync;

import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;

/**
 * Authoritative phase countdown for {@code PHASE_SYNC.countdown} (ADR-005 P-05, PRD §4.3.3).
 */
public final class PhaseCountdown {

    private static volatile boolean enabled = true;

    private PhaseCountdown() {
    }

    public static void setEnabled(boolean countdownEnabled) {
        enabled = countdownEnabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean hasTimer(GamePhase phase) {
        return durationSeconds(phase) > 0;
    }

    public static int durationSeconds(GamePhase phase) {
        return switch (phase) {
            case NIGHT_WOLF, NIGHT_WITCH -> 30;
            case NIGHT_SEER -> 20;
            case ROLE_ASSIGN -> 5;
            case NIGHT_START, NIGHT_DEATH_ANNOUNCE, EXILE_DEATH_ANNOUNCE, VOTE_RESULT -> 5;
            case LAST_WORDS -> 30;
            case DAY_DISCUSS -> 60;
            case DAY_VOTE -> 30;
            case HUNTER_SHOOT -> 20;
            case CHECK_WIN -> 1;
            default -> 0;
        };
    }

    /** Starts or restarts the deadline for the room's current phase (or per-turn phase). */
    public static void onPhaseOrTurnEntered(GameRoomState room) {
        if (!enabled) {
            room.setPhaseDeadlineEpochMs(0);
            return;
        }
        int secs = durationSeconds(room.getPhase());
        if (secs <= 0) {
            room.setPhaseDeadlineEpochMs(0);
        } else {
            room.setPhaseDeadlineEpochMs(System.currentTimeMillis() + secs * 1000L);
        }
    }

    public static boolean isExpired(GameRoomState room) {
        if (!enabled) {
            return true;
        }
        long deadline = room.getPhaseDeadlineEpochMs();
        return deadline <= 0 || System.currentTimeMillis() >= deadline;
    }

    /**
     * Remaining whole seconds (ceil), clamped to configured duration. {@code null} when phase has no timer.
     */
    public static Integer remainingSeconds(GameRoomState room) {
        if (!hasTimer(room.getPhase())) {
            return null;
        }
        if (!enabled) {
            return durationSeconds(room.getPhase());
        }
        long deadline = room.getPhaseDeadlineEpochMs();
        if (deadline <= 0) {
            return durationSeconds(room.getPhase());
        }
        long remainingMs = deadline - System.currentTimeMillis();
        int max = durationSeconds(room.getPhase());
        if (remainingMs <= 0) {
            return 0;
        }
        int secs = (int) ((remainingMs + 999) / 1000);
        return Math.min(max, secs);
    }
}

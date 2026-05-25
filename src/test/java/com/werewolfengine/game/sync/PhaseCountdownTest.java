package com.werewolfengine.game.sync;

import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseCountdownTest {

    @AfterEach
    void restoreEnabled() {
        PhaseCountdown.setEnabled(true);
    }

    @Test
    void remainingSeconds_decreasesTowardZero() {
        PhaseCountdown.setEnabled(true);
        GameRoomState room = new GameRoomState("r-countdown");
        room.setPhase(GamePhase.NIGHT_WOLF);
        assertThat(PhaseCountdown.remainingSeconds(room)).isBetween(29, 30);

        room.setPhaseDeadlineEpochMs(System.currentTimeMillis() + 500);
        assertThat(PhaseCountdown.remainingSeconds(room)).isEqualTo(1);
        assertThat(PhaseCountdown.isExpired(room)).isFalse();

        room.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1);
        assertThat(PhaseCountdown.remainingSeconds(room)).isZero();
        assertThat(PhaseCountdown.isExpired(room)).isTrue();
    }

    @Test
    void whenDisabled_alwaysExpiredAndStaticRemaining() {
        PhaseCountdown.setEnabled(false);
        GameRoomState room = new GameRoomState("r-off");
        room.setPhase(GamePhase.DAY_DISCUSS);
        assertThat(PhaseCountdown.isExpired(room)).isTrue();
        assertThat(PhaseCountdown.remainingSeconds(room)).isEqualTo(60);
    }
}

package com.werewolfengine.game.observability;

import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActionLogDedupeTest {

    @Test
    void dedupeForArchive_keepsFirstPlayerActionWithSameKey() {
        ActionLogEntry duplicate = new ActionLogEntry(
                "r1", 1, GamePhase.NIGHT_WOLF, 3, Role.WEREWOLF,
                GameActionType.KILL, 8, null, true, 1000L, null, "mock");
        ActionLogEntry system = new ActionLogEntry(
                "r1", 1, GamePhase.NIGHT_WOLF, 0, null,
                null, 8, "WOLF_KILL_RESOLVED", true, 1001L, null, null);
        List<ActionLogEntry> deduped = ActionLogService.dedupeForArchive(List.of(
                duplicate,
                duplicate,
                system
        ));
        assertThat(deduped).hasSize(2);
        assertThat(deduped.get(0).action()).isEqualTo(GameActionType.KILL);
        assertThat(deduped.get(1).action()).isNull();
    }
}

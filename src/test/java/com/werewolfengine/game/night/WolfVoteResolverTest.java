package com.werewolfengine.game.night;

import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WolfVoteResolverTest {

    @Test
    void resolveKillTarget_noVotes_picksAliveNonWolf() {
        GameRoomState room = new GameRoomState("r1");
        room.setPhase(GamePhase.NIGHT_WOLF);
        for (int i = 1; i <= 12; i++) {
            room.getPlayer(i).setRole(i <= 4 ? Role.WEREWOLF : Role.VILLAGER);
            room.getPlayer(i).setAlive(true);
        }

        int target = WolfVoteResolver.resolveKillTarget(room);

        assertThat(room.getPlayer(target).getRole()).isNotEqualTo(Role.WEREWOLF);
        assertThat(room.getPlayer(target).isAlive()).isTrue();
    }
}

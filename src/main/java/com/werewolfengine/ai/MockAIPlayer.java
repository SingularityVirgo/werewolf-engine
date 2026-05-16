package com.werewolfengine.ai;

import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Week1 Mock AI — no LLM. Wolves: random KILL on alive non-wolf in NIGHT_WOLF.
 */
@Component
public class MockAIPlayer {

    public Optional<PlayerIntent> decide(GameRoomState room, int playerId) {
        PlayerState player = room.getPlayer(playerId);
        if (player == null || !player.isAlive()) {
            return Optional.empty();
        }

        if (room.getPhase() == GamePhase.NIGHT_WOLF && player.getRole() == Role.WEREWOLF) {
            return Optional.of(randomKillNonWolf(room, playerId));
        }
        return Optional.empty();
    }

    private PlayerIntent randomKillNonWolf(GameRoomState room, int wolfId) {
        List<Integer> targets = room.alivePlayerIds().stream()
                .filter(id -> {
                    PlayerState p = room.getPlayer(id);
                    return p != null && p.getRole() != Role.WEREWOLF;
                })
                .collect(Collectors.toList());
        if (targets.isEmpty()) {
            return new PlayerIntent(GameActionType.SKIP, null, "no non-wolf target");
        }
        int target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        return new PlayerIntent(GameActionType.KILL, target, "mock random kill");
    }
}

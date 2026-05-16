package com.werewolfengine.message.payload;

import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.Role;

import java.util.List;

/** PRD §4.6.4 — per-recipient phase snapshot. */
public record PhaseSyncPayload(
        GamePhase currentPhase,
        int round,
        Integer countdown,
        List<Integer> alivePlayers,
        Role yourRole,
        List<Integer> yourTeammates,
        Boolean canAct,
        Boolean canVote,
        Boolean idiotRevealed,
        Boolean wolfChatInPhase
) {
}

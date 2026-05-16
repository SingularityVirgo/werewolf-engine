package com.werewolfengine.game;

import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PRD §3.4 step 4–6 — apply wolf kill (respect save), poison; set hunter shoot if needed.
 */
final class NightResolver {

    private NightResolver() {
    }

    /**
     * Mutates room: deaths, bottles, clears night intents, sets hunterShooterSeat if needed.
     * Caller must then set phase to HUNTER_SHOOT or DAY_ANNOUNCE / DAY_DISCUSS.
     */
    static void applyNightDeaths(GameRoomState room) {
        Integer wolfTarget = room.getPendingWolfKillTarget();
        boolean saveUsed = Boolean.TRUE.equals(room.getWitchUsedSaveTonight());
        Integer poisonTarget = room.getWitchPoisonTargetTonight();

        boolean wolfStrike = wolfTarget != null
                && room.getPlayer(wolfTarget) != null
                && room.getPlayer(wolfTarget).isAlive();

        boolean consumedAntidote = false;
        if (wolfStrike && saveUsed && room.isWitchAntidoteRemaining()) {
            wolfStrike = false;
            consumedAntidote = true;
        }

        boolean hunterMayShootFromWolf = wolfStrike
                && room.getPlayer(wolfTarget).getRole() == Role.HUNTER;

        Set<Integer> toKill = new LinkedHashSet<>();
        if (wolfStrike) {
            toKill.add(wolfTarget);
        }
        boolean poisonApplied = false;
        if (poisonTarget != null) {
            PlayerState p = room.getPlayer(poisonTarget);
            if (p != null && p.isAlive()) {
                toKill.add(poisonTarget);
                poisonApplied = true;
            }
        }

        for (int id : toKill) {
            PlayerState p = room.getPlayer(id);
            if (p != null && p.isAlive()) {
                p.setAlive(false);
            }
        }

        if (consumedAntidote) {
            room.setWitchAntidoteRemaining(false);
        }
        if (poisonApplied) {
            room.setWitchPoisonRemaining(false);
        }

        List<Integer> deaths = new ArrayList<>();
        for (int id : toKill) {
            PlayerState p = room.getPlayer(id);
            if (p != null && !p.isAlive()) {
                deaths.add(id);
            }
        }
        deaths.sort(Integer::compareTo);
        room.setLastNightDeaths(deaths);
        room.clearNightIntent();

        room.setHunterShooterSeat(null);
        if (hunterMayShootFromWolf && wolfTarget != null) {
            PlayerState h = room.getPlayer(wolfTarget);
            if (h != null && !h.isAlive()) {
                boolean alsoPoisoned = poisonTarget != null && poisonTarget.equals(wolfTarget);
                if (!alsoPoisoned) {
                    room.setHunterShooterSeat(wolfTarget);
                }
            }
        }
    }
}

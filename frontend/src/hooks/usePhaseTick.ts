import { useEffect } from 'react';
import { GamePhase } from '../types/game';

/** Phases where the backend may need periodic ticks to advance AI turns. */
const TICK_PHASES = new Set<GamePhase>([
  GamePhase.NIGHT_WOLF,
  GamePhase.NIGHT_SEER,
  GamePhase.NIGHT_WITCH,
  GamePhase.DAY_DISCUSS,
  GamePhase.DAY_VOTE,
  GamePhase.HUNTER_SHOOT,
  GamePhase.LAST_WORDS,
]);

/**
 * Backup phase tick for environments where the server-side scheduler may be disabled.
 * Primary AI advancement is handled by {@code RoomPhaseTickScheduler} on the backend.
 */
export function usePhaseTick(roomId: string, phase: GamePhase, enabled = true) {
  useEffect(() => {
    if (!enabled || !roomId || !TICK_PHASES.has(phase)) {
      return;
    }

    const intervalMs = 1500;
    const tick = async () => {
      try {
        await fetch(`/api/room/${roomId}/phase-tick`, { method: 'POST' });
      } catch {
        // Ignore transient network errors; server scheduler may still be running.
      }
    };

    const id = setInterval(tick, intervalMs);
    return () => clearInterval(id);
  }, [roomId, phase, enabled]);
}

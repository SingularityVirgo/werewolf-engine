import React from 'react';
import { GamePhase, PhaseNames, Role, RoleNames } from '../../types/game';
import { CountdownTimer } from './CountdownTimer';

interface PhaseDisplayProps {
  phase: GamePhase;
  round: number;
  yourRole: Role | null;
  countdown?: number | null;
}

const phaseStyles: Record<GamePhase, string> = {
  [GamePhase.WAITING]: 'bg-stone-surface text-text-secondary border-stone-border',
  [GamePhase.ROLE_ASSIGN]: 'bg-stone-surface text-text-primary border-ember/30',
  [GamePhase.NIGHT_START]: 'bg-stone-surface text-text-primary border-stone-border',
  [GamePhase.NIGHT_WOLF]: 'bg-stone-surface text-text-primary border-wolf/40',
  [GamePhase.NIGHT_SEER]: 'bg-stone-surface text-text-primary border-seer/40',
  [GamePhase.NIGHT_WITCH]: 'bg-stone-surface text-text-primary border-witch/40',
  [GamePhase.HUNTER_SHOOT]: 'bg-stone-surface text-text-primary border-hunter/40',
  [GamePhase.NIGHT_DEATH_ANNOUNCE]: 'bg-stone-surface text-text-secondary border-stone-border',
  [GamePhase.LAST_WORDS]: 'bg-stone-surface text-text-secondary border-stone-border',
  [GamePhase.EXILE_DEATH_ANNOUNCE]: 'bg-stone-surface text-text-secondary border-stone-border',
  [GamePhase.DAY_DISCUSS]: 'bg-stone-surface text-text-primary border-ember/20',
  [GamePhase.DAY_VOTE]: 'bg-stone-surface text-text-primary border-ember/30',
  [GamePhase.VOTE_RESULT]: 'bg-stone-surface text-text-secondary border-stone-border',
  [GamePhase.CHECK_WIN]: 'bg-stone-surface text-text-secondary border-stone-border',
  [GamePhase.GAME_OVER]: 'bg-stone-surface text-ember border-ember/40',
};

export const PhaseDisplay: React.FC<PhaseDisplayProps> = ({ phase, round, yourRole, countdown }) => {
  const isNight = phase.toString().startsWith('NIGHT') || phase === GamePhase.NIGHT_START;
  const isDay = phase.toString().startsWith('DAY');

  return (
    <div className="panel flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <span className={`phase-badge border shrink-0 ${phaseStyles[phase] || 'bg-stone-surface border-stone-border'}`}>
          {PhaseNames[phase] || phase}
        </span>

        {round > 0 && (
          <span className="text-label text-text-muted font-mono tabular-nums shrink-0">
            第 {round} 轮
          </span>
        )}

        {(isNight || isDay) && (
          <span className="text-label text-text-muted uppercase shrink-0">
            {isNight ? '夜晚' : '白天'}
          </span>
        )}
      </div>

      <div className="flex items-center gap-4 shrink-0">
        {countdown != null && <CountdownTimer countdown={countdown} />}

        {yourRole && (
          <div className="flex items-center gap-2">
            <span className="text-label text-text-muted uppercase hidden sm:inline">身份</span>
            <span className="text-body font-semibold text-ember">{RoleNames[yourRole]}</span>
          </div>
        )}
      </div>
    </div>
  );
};

import React from 'react';
import { PlayerInfo, GamePhase, Role } from '../../types/game';
import { SeatCard } from './SeatCard';

interface GameTableProps {
  players: PlayerInfo[];
  mySeatId: number | null;
  phase: GamePhase;
  phaseSync: {
    yourRole: Role | null;
    canAct: boolean;
    canVote: boolean;
  } | null;
  selectedTarget: number | null;
  onSelectTarget: (seatId: number) => void;
}

export const GameTable: React.FC<GameTableProps> = ({
  players,
  mySeatId,
  phase,
  phaseSync,
  selectedTarget,
  onSelectTarget,
}) => {
  const isNightPhase = [
    GamePhase.NIGHT_WOLF,
    GamePhase.NIGHT_SEER,
    GamePhase.NIGHT_WITCH,
    GamePhase.HUNTER_SHOOT,
  ].includes(phase);

  const isDayVote = phase === GamePhase.DAY_VOTE;

  const canSelect = (player: PlayerInfo): boolean => {
    if (!player.alive) return false;
    if (player.seatId === mySeatId) return false;
    if (isNightPhase && phaseSync?.canAct) return true;
    if (isDayVote && phaseSync?.canVote) return true;
    return false;
  };

  const showRole = phase !== GamePhase.WAITING;

  return (
    <div className="panel">
      <h3 className="text-label text-text-muted uppercase tracking-wider mb-4 text-center">
        座位表
      </h3>

      <div className="seat-grid">
        {players.map((player) => (
          <SeatCard
            key={player.seatId}
            player={player}
            isMySeat={player.seatId === mySeatId}
            isSelected={selectedTarget === player.seatId}
            isSpeaking={false}
            selectable={canSelect(player)}
            showRole={showRole}
            onClick={() => canSelect(player) && onSelectTarget(player.seatId)}
          />
        ))}
      </div>
    </div>
  );
};

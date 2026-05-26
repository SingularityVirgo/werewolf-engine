import React from 'react';
import { PlayerInfo } from '../../types/game';
import { RoleBadge } from './RoleBadge';

interface SeatCardProps {
  player: PlayerInfo;
  isMySeat: boolean;
  isSelected: boolean;
  isSpeaking: boolean;
  selectable: boolean;
  showRole: boolean;
  onClick?: () => void;
}

export const SeatCard: React.FC<SeatCardProps> = ({
  player,
  isMySeat,
  isSelected,
  isSpeaking,
  selectable,
  showRole,
  onClick,
}) => {
  const { seatId, alive, ready, role, isHuman } = player;

  return (
    <button
      type="button"
      className={`
        seat-tile flex flex-col items-center justify-center min-h-[72px]
        ${!alive ? 'opacity-50' : ''}
        ${isMySeat ? 'ring-2 ring-ember ring-offset-1 ring-offset-abyss' : ''}
        ${isSpeaking && alive ? 'seat-speaking' : ''}
        ${isSelected ? 'ring-2 ring-blood ring-offset-1 ring-offset-abyss bg-blood/10 border-blood/40' : ''}
        ${selectable && alive ? 'cursor-pointer hover:border-ember/40 hover:bg-stone' : 'cursor-default'}
      `}
      onClick={selectable && alive ? onClick : undefined}
      disabled={!selectable || !alive}
      aria-label={`座位 ${seatId}${isSpeaking ? '，发言中' : ''}`}
    >
      <div className="absolute top-1.5 left-1.5 text-label text-text-muted font-mono tabular-nums">
        #{seatId}
      </div>

      {ready && alive && (
        <div className="absolute top-1.5 right-1.5 w-1.5 h-1.5 rounded-full bg-ember" aria-label="已准备" />
      )}

      <div className={`font-mono text-body font-semibold tabular-nums ${!alive ? 'text-text-muted line-through' : 'text-text-primary'}`}>
        {seatId}
      </div>

      <div className="text-label text-text-secondary mt-0.5 truncate max-w-full px-1">
        {isHuman ? `玩家 ${seatId}` : `AI ${seatId}`}
      </div>

      {showRole && role && alive && (
        <div className="mt-1">
          <RoleBadge role={role} size="sm" />
        </div>
      )}

      {isSpeaking && alive && (
        <div className="absolute -top-2 left-1/2 -translate-x-1/2 bg-ember text-abyss text-label font-semibold px-2 py-0.5 rounded-md whitespace-nowrap">
          发言中
        </div>
      )}

      {!alive && (
        <div className="absolute inset-0 flex items-center justify-center bg-abyss/70 rounded-lg">
          <span className="text-label font-semibold text-blood uppercase tracking-wider">出局</span>
        </div>
      )}

      {isMySeat && alive && !isSpeaking && (
        <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-ember text-abyss text-label font-semibold px-2 py-0.5 rounded-md">
          我
        </div>
      )}
    </button>
  );
};

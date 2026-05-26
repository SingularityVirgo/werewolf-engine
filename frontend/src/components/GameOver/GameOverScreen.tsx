import React from 'react';
import { GameWinner, Role } from '../../types/game';
import { RoleBadge } from '../GameBoard/RoleBadge';
import { OrnatePanel } from '../brand/OrnatePanel';

interface GameOverScreenProps {
  winner: GameWinner | null;
  roles: Record<number, Role>;
  onBackToLobby: () => void;
}

export const GameOverScreen: React.FC<GameOverScreenProps> = ({
  winner,
  roles,
  onBackToLobby,
}) => {
  const isVillagerWin = winner === GameWinner.VILLAGERS;
  const hasWinner = winner !== null;

  const title = hasWinner
    ? isVillagerWin
      ? '好人阵营获胜'
      : '狼人阵营获胜'
    : '对局结束';

  const subtitle = hasWinner
    ? isVillagerWin
      ? '所有狼人已被消灭'
      : '狼人已占领村庄'
    : '身份揭晓';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-abyss/92">
      <OrnatePanel tone="parchment" className="max-w-lg w-full mx-4 animate-fade-in">
        <header className="text-center mb-8 space-y-3 pt-2">
          <p className="text-label text-text-muted uppercase tracking-[0.2em]">裁决</p>
          <h1
            className={`font-display text-display font-semibold ${
              hasWinner ? (isVillagerWin ? 'text-ember' : 'text-blood') : 'text-ember'
            }`}
          >
            {title}
          </h1>
          <p className="text-body text-text-secondary">{subtitle}</p>
        </header>

        {Object.keys(roles).length > 0 && (
          <div className="mb-8">
            <h3 className="text-label text-text-muted uppercase tracking-wider mb-4 text-center">
              身份揭晓
            </h3>
            <div className="grid grid-cols-4 gap-2">
              {Object.entries(roles)
                .sort(([a], [b]) => Number(a) - Number(b))
                .map(([seatId, role]) => (
                  <div
                    key={seatId}
                    className="bg-stone-surface/80 border border-stone-border rounded-sm p-2 text-center space-y-1"
                  >
                    <div className="font-mono text-label text-text-muted tabular-nums">#{seatId}</div>
                    <RoleBadge role={role} size="sm" />
                  </div>
                ))}
            </div>
          </div>
        )}

        <button type="button" className="btn-primary w-full" onClick={onBackToLobby}>
          返回大厅
        </button>
      </OrnatePanel>
    </div>
  );
};

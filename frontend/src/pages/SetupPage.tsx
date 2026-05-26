import React, { useState } from 'react';
import { api } from '../services/api';
import { BOARD_PRESETS, BOARD_TYPE_STANDARD } from '../constants/boardTypes';
import { BrandBackdrop } from '../components/brand/BrandBackdrop';
import { OrnatePanel } from '../components/brand/OrnatePanel';

interface SetupPageProps {
  onBack: () => void;
  onCreated: (roomId: string, seatId: number, userId: number) => void;
}

export const SetupPage: React.FC<SetupPageProps> = ({ onBack, onCreated }) => {
  const [selectedBoard] = useState(BOARD_TYPE_STANDARD);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCreate = async () => {
    setLoading(true);
    setError('');
    const userId = Math.floor(Math.random() * 9000 + 1000);
    try {
      const res = await api.createRoom({
        boardType: selectedBoard,
        aiCount: 11,
        hostUserId: userId,
      });
      onCreated(res.roomId, 1, userId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '创建房间失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <BrandBackdrop variant="setup">
      <div className="min-h-screen px-4 py-8 lg:py-12 animate-fade-in">
        <div className="max-w-lg mx-auto space-y-8">
          <header className="flex items-end justify-between gap-4 border-b border-stone-border/50 pb-6">
            <div>
              <p className="text-label text-mist uppercase tracking-[0.15em] mb-2">牌室门厅</p>
              <h1 className="font-display text-title lg:text-2xl font-semibold text-text-primary">
                选择板子
              </h1>
            </div>
            <button type="button" className="btn-secondary text-body shrink-0" onClick={onBack}>
              返回
            </button>
          </header>

          <div className="space-y-4">
            {BOARD_PRESETS.map((board) => (
              <OrnatePanel
                key={board.id}
                tone={board.enabled ? 'parchment' : 'stone'}
                className={`transition-opacity ${!board.enabled ? 'opacity-45' : ''} ${
                  board.enabled && board.id === selectedBoard ? 'ring-1 ring-ember/30' : ''
                }`}
              >
                <div className="flex items-start justify-between gap-3 mb-2">
                  <h2 className="font-display text-title font-semibold text-text-primary">
                    {board.name}
                  </h2>
                  {board.enabled && board.id === selectedBoard && (
                    <span className="text-label text-ember uppercase shrink-0 pt-1">已选</span>
                  )}
                  {!board.enabled && (
                    <span className="text-label text-text-muted uppercase shrink-0 pt-1">即将开放</span>
                  )}
                </div>
                <p className="text-body text-text-secondary mb-4">{board.description}</p>
                {board.enabled && (
                  <button
                    type="button"
                    className="btn-primary w-full"
                    onClick={handleCreate}
                    disabled={loading}
                  >
                    {loading ? '创建中…' : '创建房间'}
                  </button>
                )}
              </OrnatePanel>
            ))}
          </div>

          {error && (
            <p className="text-body text-blood text-center" role="alert">
              {error}
            </p>
          )}
        </div>
      </div>
    </BrandBackdrop>
  );
};

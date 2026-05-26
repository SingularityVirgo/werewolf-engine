import React, { useState, useCallback, useEffect } from 'react';

import { useGameState } from '../hooks/useGameState';
import { useWebSocket } from '../hooks/useWebSocket';
import { usePhaseTick } from '../hooks/usePhaseTick';
import { api } from '../services/api';
import { PhaseDisplay } from './PhaseBar/PhaseDisplay';
import { GameTableSideLayout } from './GameBoard/GameTableSideLayout';
import { ActionPanel } from './ActionPanel/ActionPanel';
import { GameOverScreen } from './GameOver/GameOverScreen';
import { Loading } from './common/Loading';
import { BrandBackdrop } from './brand/BrandBackdrop';
import { Toast } from './common/Toast';
import { GamePhase, PhaseSyncPayload, GameOverPayload, ChatMessagePayload, GameEventPayload } from '../types/game';

interface GamePageProps {
  roomId: string;
  seatId: number;
  userId: number;
  isOwner: boolean;
  onBackToHome: () => void;
}

function phaseAmbientClass(phase: GamePhase): string {
  if (phase.toString().startsWith('NIGHT') || phase === GamePhase.NIGHT_START) {
    return 'bg-abyss';
  }
  if (phase.toString().startsWith('DAY')) {
    return 'bg-[#0f1412]';
  }
  return 'bg-abyss';
}

export const GamePage: React.FC<GamePageProps> = ({
  roomId,
  seatId,
  userId,
  isOwner,
  onBackToHome,
}) => {
  const {
    state,
    setRoom,
    setMySeat,
    setConnected,
    updatePlayerReady,
    handlePhaseSync,
    handleActionAck,
    handleGameOver,
    handleGameEvent,
    handleChatMessage,
    reset,
  } = useGameState();

  const [selectedTarget, setSelectedTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<{ message: string; type: 'info' | 'error' | 'success' } | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setRoom(roomId, isOwner);
    setMySeat(seatId, userId);
  }, [roomId, seatId, userId, isOwner, setRoom, setMySeat]);

  const onConnected = useCallback(() => {
    setConnected(true);
    setLoading(false);
    setToast({ message: '已连接', type: 'success' });
  }, [setConnected]);

  const onPhaseSync = useCallback((payload: PhaseSyncPayload) => {
    handlePhaseSync(payload);
    setSelectedTarget(null);
  }, [handlePhaseSync]);

  const onActionAck = useCallback((payload: any) => {
    handleActionAck(payload);
    if (!payload.success) {
      setToast({ message: payload.message || '操作失败', type: 'error' });
    }
  }, [handleActionAck]);

  const onGameOver = useCallback((payload: GameOverPayload) => {
    handleGameOver(payload);
  }, [handleGameOver]);

  const onChatMessage = useCallback((payload: ChatMessagePayload) => {
    handleChatMessage(payload);
  }, [handleChatMessage]);

  const onGameEvent = useCallback((payload: GameEventPayload) => {
    handleGameEvent(payload);
  }, [handleGameEvent]);

  const onWsError = useCallback((message: string) => {
    setToast({ message, type: 'error' });
  }, []);

  usePhaseTick(roomId, state.phase, !loading && state.phase !== GamePhase.WAITING);

  const ws = useWebSocket({
    userId,
    roomId,
    seatId,
    onConnected,
    onPhaseSync,
    onActionAck,
    onGameOver,
    onChatMessage,
    onGameEvent,
    onError: onWsError,
  });

  const handleReady = async () => {
    try {
      await api.setReady(roomId, seatId, true);
      updatePlayerReady(seatId, true);
      ws.setReady(roomId, seatId, true);
      setToast({ message: '已准备', type: 'success' });
    } catch (e: unknown) {
      setToast({ message: e instanceof Error ? e.message : '准备失败', type: 'error' });
    }
  };

  const handleStart = async () => {
    try {
      const result = await api.startGame(roomId, userId);
      if (result.success) {
        setToast({ message: '对局已开始', type: 'success' });
      } else {
        setToast({ message: result.message || '开局失败', type: 'error' });
      }
    } catch (e: unknown) {
      setToast({ message: e instanceof Error ? e.message : '开局失败', type: 'error' });
    }
  };

  const handleAction = useCallback(
    (action: string, target?: number, content?: string) => {
      ws.sendGameAction(roomId, seatId, action, state.phase, target, content);
      setSelectedTarget(null);
    },
    [ws, roomId, seatId, state.phase]
  );

  const handleSendMessage = useCallback(
    (content: string, isWolfChat: boolean) => {
      ws.sendGameAction(roomId, seatId, isWolfChat ? 'WOLF_CHAT' : 'SPEAK', state.phase, undefined, content);
    },
    [ws, roomId, seatId, state.phase]
  );

  const handleExit = () => {
    ws.disconnect();
    reset();
    onBackToHome();
  };

  if (loading) {
    return <Loading text="正在连接…" />;
  }

  const isWaiting = state.phase === GamePhase.WAITING;
  const isGameOver = state.phase === GamePhase.GAME_OVER;

  return (
    <BrandBackdrop variant="game">
      <div className={`min-h-screen transition-colors duration-500 ${phaseAmbientClass(state.phase)}`}>
        {toast && (
          <Toast message={toast.message} type={toast.type} onDone={() => setToast(null)} />
        )}

        <div className="max-w-5xl mx-auto p-3 lg:p-5 space-y-3">
        <header className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="text-label text-text-muted font-mono truncate">房间 {roomId}</p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            {isWaiting && (
              <>
                <button type="button" className="btn-primary text-body" onClick={handleReady}>
                  准备
                </button>
                {isOwner && (
                  <button type="button" className="btn-danger text-body" onClick={handleStart}>
                    开始
                  </button>
                )}
              </>
            )}
            <button type="button" className="btn-secondary text-body" onClick={handleExit}>
              退出
            </button>
          </div>
        </header>

        <PhaseDisplay
          phase={state.phase}
          round={state.round}
          yourRole={state.phaseSync?.yourRole || null}
          countdown={state.phaseSync?.countdown}
        />

        <GameTableSideLayout
          players={state.players}
          mySeatId={state.mySeatId}
          phase={state.phase}
          roomId={roomId}
          round={state.round}
          phaseSync={
            state.phaseSync
              ? {
                  yourRole: state.phaseSync.yourRole,
                  canAct: state.phaseSync.canAct,
                  canVote: state.phaseSync.canVote,
                  currentSpeakerId: state.phaseSync.currentSpeakerId,
                  wolfChatInPhase: state.phaseSync.wolfChatInPhase,
                }
              : null
          }
          messages={state.chatMessages}
          gameLog={state.gameLog}
          selectedTarget={selectedTarget}
          onSelectTarget={setSelectedTarget}
          onSendMessage={handleSendMessage}
        />

        <ActionPanel
          phase={state.phase}
          myRole={state.phaseSync?.yourRole || null}
          phaseSync={state.phaseSync}
          selectedTarget={selectedTarget}
          onAction={handleAction}
        />
        </div>

        {isGameOver && (
          <GameOverScreen
            winner={state.winner}
            roles={state.finalRoles || {}}
            onBackToLobby={handleExit}
          />
        )}
      </div>
    </BrandBackdrop>
  );
};

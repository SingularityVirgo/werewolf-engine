import React from 'react';
import { PlayerInfo, GamePhase, Role } from '../../types/game';
import { SeatCard } from './SeatCard';
import { CenterStage } from './CenterStage';
import { ChatMessage, GameLogEntry } from '../../types/game';

interface GameTableSideLayoutProps {
  players: PlayerInfo[];
  mySeatId: number | null;
  phase: GamePhase;
  roomId: string;
  round: number;
  phaseSync: {
    yourRole: Role | null;
    canAct: boolean;
    canVote: boolean;
    currentSpeakerId: number | null;
    wolfChatInPhase: boolean;
  } | null;
  messages: ChatMessage[];
  gameLog: GameLogEntry[];
  selectedTarget: number | null;
  onSelectTarget: (seatId: number) => void;
  onSendMessage: (content: string, isWolfChat: boolean) => void;
}

export const GameTableSideLayout: React.FC<GameTableSideLayoutProps> = ({
  players,
  mySeatId,
  phase,
  roomId,
  round,
  phaseSync,
  messages,
  gameLog,
  selectedTarget,
  onSelectTarget,
  onSendMessage,
}) => {
  const isNightPhase = [
    GamePhase.NIGHT_WOLF,
    GamePhase.NIGHT_SEER,
    GamePhase.NIGHT_WITCH,
    GamePhase.HUNTER_SHOOT,
  ].includes(phase);

  const isDayVote = phase === GamePhase.DAY_VOTE;
  const currentSpeakerId = phaseSync?.currentSpeakerId ?? null;

  const canSelect = (player: PlayerInfo): boolean => {
    if (!player.alive) return false;
    if (player.seatId === mySeatId) return false;
    if (isNightPhase && phaseSync?.canAct) return true;
    if (isDayVote && phaseSync?.canVote) return true;
    return false;
  };

  const showRole = phase !== GamePhase.WAITING;
  const leftSeats = players.filter((p) => p.seatId >= 1 && p.seatId <= 6);
  const rightSeats = players.filter((p) => p.seatId >= 7 && p.seatId <= 12);

  const renderSeat = (player: PlayerInfo) => (
    <SeatCard
      key={player.seatId}
      player={player}
      isMySeat={player.seatId === mySeatId}
      isSelected={selectedTarget === player.seatId}
      isSpeaking={currentSpeakerId === player.seatId}
      selectable={canSelect(player)}
      showRole={showRole}
      onClick={() => canSelect(player) && onSelectTarget(player.seatId)}
    />
  );

  return (
    <div className="grid grid-cols-[minmax(88px,1fr)_minmax(240px,2fr)_minmax(88px,1fr)] gap-3 lg:gap-4 items-stretch">
      <div className="flex flex-col gap-2">{leftSeats.map(renderSeat)}</div>

      <CenterStage
        phase={phase}
        roomId={roomId}
        round={round}
        messages={messages}
        gameLog={gameLog}
        myRole={phaseSync?.yourRole ?? null}
        mySeatId={mySeatId}
        currentSpeakerId={currentSpeakerId}
        wolfChatInPhase={phaseSync?.wolfChatInPhase ?? false}
        onSendMessage={onSendMessage}
      />

      <div className="flex flex-col gap-2">{rightSeats.map(renderSeat)}</div>
    </div>
  );
};

import React from 'react';
import { GamePhase, Role, PhaseSyncPayload, PlayerSubStateValue } from '../../types/game';
import { NightWolfAction } from './NightWolfAction';
import { NightSeerAction } from './NightSeerAction';
import { NightWitchAction } from './NightWitchAction';
import { HunterAction } from './HunterAction';
import { DayVoteAction } from './DayVoteAction';
import { DayDiscussAction } from './DayDiscussAction';

interface ActionPanelProps {
  phase: GamePhase;
  myRole: Role | null;
  phaseSync: PhaseSyncPayload | null;
  selectedTarget: number | null;
  playerSubState: PlayerSubStateValue;
  onAction: (action: string, target?: number, content?: string) => void;
}

const waitingMessages: Partial<Record<GamePhase, string>> = {
  [GamePhase.DAY_DISCUSS]: '白天讨论，等待轮到你发言',
  [GamePhase.NIGHT_DEATH_ANNOUNCE]: '公布昨夜死讯',
  [GamePhase.EXILE_DEATH_ANNOUNCE]: '公布放逐结果',
  [GamePhase.LAST_WORDS]: '遗言阶段',
  [GamePhase.VOTE_RESULT]: '统计投票结果',
  [GamePhase.CHECK_WIN]: '胜负判定中',
  [GamePhase.GAME_OVER]: '对局已结束',
};

export const ActionPanel: React.FC<ActionPanelProps> = ({
  phase,
  myRole,
  phaseSync,
  selectedTarget,
  playerSubState,
  onAction,
}) => {
  if (!phaseSync?.canAct && phase !== GamePhase.DAY_VOTE) {
    return null;
  }

  const renderAction = () => {
    switch (phase) {
      case GamePhase.NIGHT_WOLF:
        if (myRole === Role.WEREWOLF) {
          return (
            <NightWolfAction
              selectedTarget={selectedTarget}
              wolfChatInPhase={phaseSync?.wolfChatInPhase || false}
              playerSubState={playerSubState}
              onKill={() => onAction('KILL', selectedTarget || undefined)}
              onWolfChat={(content) => onAction('WOLF_CHAT', undefined, content)}
              onSkip={() => onAction('SKIP')}
            />
          );
        }
        break;

      case GamePhase.NIGHT_SEER:
        if (myRole === Role.SEER) {
          return (
            <NightSeerAction
              selectedTarget={selectedTarget}
              onCheck={() => onAction('CHECK', selectedTarget || undefined)}
              onSkip={() => onAction('SKIP')}
            />
          );
        }
        break;

      case GamePhase.NIGHT_WITCH:
        if (myRole === Role.WITCH) {
          return (
            <NightWitchAction
              selectedTarget={selectedTarget}
              wolfKillTarget={phaseSync?.wolfKillTarget || null}
              antidoteLeft={phaseSync?.witchAntidoteLeft || 0}
              poisonLeft={phaseSync?.witchPoisonLeft || 0}
              onSave={() => onAction('SAVE')}
              onPoison={() => onAction('POISON', selectedTarget || undefined)}
              onSkip={() => onAction('SKIP')}
            />
          );
        }
        break;

      case GamePhase.HUNTER_SHOOT:
        if (myRole === Role.HUNTER) {
          return (
            <HunterAction
              selectedTarget={selectedTarget}
              onShoot={() => onAction('SHOOT', selectedTarget || undefined)}
              onSkip={() => onAction('SKIP')}
            />
          );
        }
        break;

      case GamePhase.DAY_DISCUSS:
        if (phaseSync?.canAct) {
          return <DayDiscussAction onSkipSpeak={() => onAction('SKIP_SPEAK')} />;
        }
        break;

      case GamePhase.DAY_VOTE:
        if (phaseSync?.canVote) {
          return (
            <DayVoteAction
              selectedTarget={selectedTarget}
              onVote={() => onAction('VOTE', selectedTarget || undefined)}
              onSkipVote={() => onAction('SKIP_VOTE')}
            />
          );
        }
        break;
    }

    const message = waitingMessages[phase] || '等待行动';
    return (
      <div className="panel text-center py-6">
        <p className="text-body text-text-secondary">{message}</p>
      </div>
    );
  };

  return (
    <div className="animate-fade-in">
      {renderAction()}
    </div>
  );
};

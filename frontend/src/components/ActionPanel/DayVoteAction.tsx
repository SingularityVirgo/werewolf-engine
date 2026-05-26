import React from 'react';

interface DayVoteActionProps {
  selectedTarget: number | null;
  onVote: () => void;
  onSkipVote: () => void;
}

export const DayVoteAction: React.FC<DayVoteActionProps> = ({
  selectedTarget,
  onVote,
  onSkipVote,
}) => {
  return (
    <div className="panel space-y-4">
      <h3 className="text-title font-semibold text-text-primary">投票放逐</h3>
      <p className="text-body text-text-secondary">
        {selectedTarget
          ? <>投票 <span className="font-mono text-gold">#{selectedTarget}</span></>
          : '在座位表中选择放逐目标'}
      </p>
      <div className="flex gap-2">
        <button
          className="btn-primary flex-1"
          onClick={onVote}
          disabled={!selectedTarget}
        >
          投票
        </button>
        <button className="btn-secondary flex-1" onClick={onSkipVote}>
          弃票
        </button>
      </div>
    </div>
  );
};

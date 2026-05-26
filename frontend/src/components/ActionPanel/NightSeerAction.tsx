import React from 'react';

interface NightSeerActionProps {
  selectedTarget: number | null;
  onCheck: () => void;
  onSkip: () => void;
}

export const NightSeerAction: React.FC<NightSeerActionProps> = ({
  selectedTarget,
  onCheck,
  onSkip,
}) => {
  return (
    <div className="panel space-y-4">
      <h3 className="text-title font-semibold text-text-primary">预言家行动</h3>
      <p className="text-body text-text-secondary">
        {selectedTarget
          ? <>查验 <span className="font-mono text-gold">#{selectedTarget}</span></>
          : '在座位表中选择查验目标'}
      </p>
      <div className="flex gap-2">
        <button
          className="btn-primary flex-1"
          onClick={onCheck}
          disabled={!selectedTarget}
        >
          查验
        </button>
        <button className="btn-secondary flex-1" onClick={onSkip}>
          跳过
        </button>
      </div>
    </div>
  );
};

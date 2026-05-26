import React from 'react';

interface HunterActionProps {
  selectedTarget: number | null;
  onShoot: () => void;
  onSkip: () => void;
}

export const HunterAction: React.FC<HunterActionProps> = ({
  selectedTarget,
  onShoot,
  onSkip,
}) => {
  return (
    <div className="panel space-y-4">
      <h3 className="text-title font-semibold text-text-primary">猎人开枪</h3>
      <p className="text-body text-text-secondary">
        你被击杀，可以开枪带走一名玩家
      </p>
      <p className="text-body text-text-secondary">
        {selectedTarget
          ? <>目标 <span className="font-mono text-gold">#{selectedTarget}</span></>
          : '在座位表中选择目标'}
      </p>
      <div className="flex gap-2">
        <button
          className="btn-danger flex-1"
          onClick={onShoot}
          disabled={!selectedTarget}
        >
          开枪
        </button>
        <button className="btn-secondary flex-1" onClick={onSkip}>
          不开枪
        </button>
      </div>
    </div>
  );
};

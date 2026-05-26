import React from 'react';

interface NightWitchActionProps {
  selectedTarget: number | null;
  wolfKillTarget: number | null;
  antidoteLeft: number;
  poisonLeft: number;
  onSave: () => void;
  onPoison: () => void;
  onSkip: () => void;
}

export const NightWitchAction: React.FC<NightWitchActionProps> = ({
  selectedTarget,
  wolfKillTarget,
  antidoteLeft,
  poisonLeft,
  onSave,
  onPoison,
  onSkip,
}) => {
  return (
    <div className="panel space-y-4">
      <h3 className="text-title font-semibold text-text-primary">女巫行动</h3>

      {wolfKillTarget && antidoteLeft > 0 && (
        <div className="bg-night-surface border border-witch/30 rounded-md p-3">
          <p className="text-body text-text-secondary">
            今夜刀口：<span className="font-mono font-semibold text-text-primary">#{wolfKillTarget}</span>
          </p>
        </div>
      )}

      <p className="text-label text-text-muted">
        解药 {antidoteLeft > 0 ? '可用' : '已用'} · 毒药 {poisonLeft > 0 ? '可用' : '已用'}
      </p>

      <p className="text-body text-text-secondary">
        {selectedTarget
          ? <>毒杀目标 <span className="font-mono text-gold">#{selectedTarget}</span></>
          : '选择目标后使用毒药，或直接救/跳过'}
      </p>

      <div className="flex gap-2">
        <button
          className="btn-primary flex-1"
          onClick={onSave}
          disabled={!wolfKillTarget || antidoteLeft <= 0}
        >
          救
        </button>
        <button
          className="btn-danger flex-1"
          onClick={onPoison}
          disabled={!selectedTarget || poisonLeft <= 0}
        >
          毒
        </button>
        <button className="btn-secondary flex-1" onClick={onSkip}>
          跳过
        </button>
      </div>
    </div>
  );
};

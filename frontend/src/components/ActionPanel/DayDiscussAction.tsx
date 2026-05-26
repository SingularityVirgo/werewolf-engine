import React from 'react';

interface DayDiscussActionProps {
  onSkipSpeak: () => void;
}

export const DayDiscussAction: React.FC<DayDiscussActionProps> = ({ onSkipSpeak }) => {
  return (
    <div className="panel space-y-3">
      <h3 className="text-title font-semibold text-text-primary">轮到你发言</h3>
      <p className="text-body text-text-secondary">
        在公频输入发言内容，或点击下方跳过。
      </p>
      <button type="button" className="btn-secondary w-full" onClick={onSkipSpeak}>
        跳过发言
      </button>
    </div>
  );
};

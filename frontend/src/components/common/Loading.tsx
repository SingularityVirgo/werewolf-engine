import React from 'react';
import { CandleIcon } from '../brand/CandleIcon';

export const Loading: React.FC<{ text?: string }> = ({ text = '加载中…' }) => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-5 bg-abyss">
      <CandleIcon size={36} className="text-ember candle-glow" />
      <p className="text-body text-text-secondary">{text}</p>
    </div>
  );
};

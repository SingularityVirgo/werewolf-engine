import React, { useState } from 'react';

interface NightWolfActionProps {
  selectedTarget: number | null;
  wolfChatInPhase: boolean;
  onKill: () => void;
  onWolfChat: (content: string) => void;
  onSkip: () => void;
}

export const NightWolfAction: React.FC<NightWolfActionProps> = ({
  selectedTarget,
  wolfChatInPhase,
  onKill,
  onWolfChat,
  onSkip,
}) => {
  const [wolfInput, setWolfInput] = useState('');

  const handleWolfSend = () => {
    const text = wolfInput.trim();
    if (!text) return;
    onWolfChat(text);
    setWolfInput('');
  };

  return (
    <div className="panel space-y-4">
      <h3 className="text-title font-semibold text-text-primary">狼人行动</h3>
      <p className="text-body text-text-secondary">
        {selectedTarget
          ? <>目标 <span className="font-mono text-gold">#{selectedTarget}</span></>
          : '在座位表中选择目标'}
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          className="btn-danger flex-1"
          onClick={onKill}
          disabled={!selectedTarget}
        >
          击杀
        </button>
        <button type="button" className="btn-secondary flex-1" onClick={onSkip}>
          跳过
        </button>
      </div>
      <div className="space-y-2 pt-2 border-t border-stone-border">
        <p className="text-label text-text-muted uppercase">
          {wolfChatInPhase ? '狼频道（商议后可刀队友）' : '狼频道商议'}
        </p>
        <div className="flex gap-2">
          <input
            className="input-field flex-1 text-body"
            placeholder="狼频道发言…"
            value={wolfInput}
            onChange={(e) => setWolfInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleWolfSend();
              }
            }}
          />
          <button type="button" className="btn-secondary text-body px-4" onClick={handleWolfSend}>
            发送
          </button>
        </div>
      </div>
    </div>
  );
};

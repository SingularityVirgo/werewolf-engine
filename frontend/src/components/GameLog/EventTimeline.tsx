import React, { useRef, useEffect } from 'react';
import { GameLogEntry } from '../../types/game';

interface EventTimelineProps {
  entries: GameLogEntry[];
}

const typeStyles: Record<string, string> = {
  system: 'text-text-muted',
  action: 'text-text-secondary',
  death: 'text-blood',
  vote: 'text-gold-muted',
  event: 'text-text-secondary',
};

export const EventTimeline: React.FC<EventTimelineProps> = ({ entries }) => {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [entries]);

  return (
    <div className="panel">
      <h3 className="text-label text-text-muted uppercase tracking-wider mb-3">事件日志</h3>
      <div className="max-h-48 overflow-y-auto space-y-2">
        {entries.length === 0 && (
          <p className="text-label text-text-muted text-center py-6">暂无记录</p>
        )}
        {entries.map((entry) => (
          <div key={entry.id} className="flex items-start gap-2 text-label">
            {entry.round > 0 && (
              <span className="font-mono text-text-muted tabular-nums shrink-0 w-8">
                R{entry.round}
              </span>
            )}
            <span className={typeStyles[entry.type] || 'text-text-muted'}>
              {entry.message}
            </span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
    </div>
  );
};

import { GameEventPayload } from '../types/game';

function seatFromData(data: Record<string, unknown>): number | null {
  const raw = data.playerId ?? data.seatId ?? data.seat ?? data.exiled;
  return raw != null ? Number(raw) : null;
}

const EVENT_LABELS: Record<string, (data: Record<string, unknown>) => string> = {
  NIGHT_DEATHS: (data) => {
    const deaths = data.deaths ?? data.playerIds ?? data.seats;
    if (Array.isArray(deaths) && deaths.length > 0) {
      return `昨夜出局：${deaths.map((d) => `#${d}`).join('、')}`;
    }
    return '昨夜平安，无人出局';
  },
  EXILE_ANNOUNCED: (data) => {
    const seat = seatFromData(data);
    return seat != null ? `#${seat} 被投票放逐` : '有人被投票放逐';
  },
  HUNTER_SHOT: (data) => {
    const hunter = seatFromData(data);
    const target = data.target ?? data.targetId;
    if (data.skipped === true) {
      return hunter != null ? `#${hunter} 猎人未开枪` : '猎人未开枪';
    }
    if (target != null) {
      return hunter != null
        ? `#${hunter} 猎人开枪带走 #${target}`
        : `猎人开枪带走 #${target}`;
    }
    return hunter != null ? `#${hunter} 猎人开枪` : '猎人开枪';
  },
  IDIOT_REVEALED: (data) => {
    const seat = seatFromData(data);
    return seat != null ? `#${seat} 愚者翻牌，不离场` : '愚者翻牌';
  },
};

export function formatGameEvent(payload: GameEventPayload): string {
  const formatter = EVENT_LABELS[payload.type];
  if (formatter) {
    return formatter(payload.data);
  }
  return `事件：${payload.type}`;
}

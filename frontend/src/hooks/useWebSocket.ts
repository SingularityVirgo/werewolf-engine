import { useEffect, useRef, useCallback } from 'react';
import { WsClient } from '../services/ws';
import {
  WsEnvelope,
  MessageType,
  PhaseSyncPayload,
  ActionAckPayload,
  GameOverPayload,
  ChatMessagePayload,
  GameEventPayload,
  GameWinner,
  Role,
} from '../types/game';

interface UseWebSocketOptions {
  userId: number;
  /** When set, JOIN_ROOM is sent automatically after the gateway CONNECTED ack. */
  roomId?: string;
  seatId?: number;
  /** Connect on mount and disconnect on unmount (default true). */
  autoConnect?: boolean;
  onConnected?: () => void;
  onPhaseSync?: (payload: PhaseSyncPayload) => void;
  onActionAck?: (payload: ActionAckPayload) => void;
  onGameOver?: (payload: GameOverPayload) => void;
  onChatMessage?: (payload: ChatMessagePayload) => void;
  onGameEvent?: (payload: GameEventPayload) => void;
  onError?: (message: string) => void;
}

function normalizeChatPayload(raw: Record<string, unknown>): ChatMessagePayload {
  const scope = String(raw.scope ?? 'ALL');
  const playerId = Number(raw.playerId ?? raw.seatId ?? 0);
  return {
    seatId: playerId,
    content: String(raw.content ?? ''),
    isWolfChat: scope === 'WEREWOLF',
  };
}

function normalizeGameOverPayload(raw: Record<string, unknown>): GameOverPayload {
  const winnerRaw = String(raw.winner ?? '');
  const winner =
    winnerRaw === 'WEREWOLVES' || winnerRaw === GameWinner.WEREWOLVES
      ? GameWinner.WEREWOLVES
      : winnerRaw === 'VILLAGERS' || winnerRaw === GameWinner.VILLAGERS
        ? GameWinner.VILLAGERS
        : null;

  const roles: Record<number, Role> = {};
  const players = raw.players as Array<{ playerId: number; role: string }> | undefined;
  if (Array.isArray(players)) {
    for (const p of players) {
      if (p.role) roles[p.playerId] = p.role as Role;
    }
  }
  return { winner: winner ?? GameWinner.VILLAGERS, roles };
}

export function useWebSocket(options: UseWebSocketOptions) {
  const clientRef = useRef<WsClient | null>(null);
  const optionsRef = useRef(options);
  optionsRef.current = options;

  const connect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.disconnect();
    }

    const wsUrl = window.location.origin;
    const client = new WsClient(wsUrl, String(options.userId));
    clientRef.current = client;

    client.onMessage((envelope: WsEnvelope) => {
      const { type, payload } = envelope;
      const opts = optionsRef.current;
      const raw = payload as Record<string, unknown>;

      switch (type) {
        case MessageType.CONNECTED: {
          const { roomId, seatId } = opts;
          if (roomId != null && seatId != null) {
            clientRef.current?.send(MessageType.JOIN_ROOM, {
              roomId,
              seatId,
              userId: opts.userId,
            });
          }
          opts.onConnected?.();
          break;
        }

        case MessageType.PHASE_SYNC: {
          const phaseSync = raw?.phaseSync as PhaseSyncPayload | undefined;
          if (phaseSync) {
            opts.onPhaseSync?.(phaseSync);
          }
          break;
        }

        case MessageType.ACTION_ACK: {
          const ackPayload = raw?.ack as ActionAckPayload | undefined;
          if (ackPayload) {
            opts.onActionAck?.(ackPayload);
          }
          break;
        }

        case MessageType.GAME_OVER:
          opts.onGameOver?.(normalizeGameOverPayload(raw));
          break;

        case MessageType.CHAT_BROADCAST:
          opts.onChatMessage?.(normalizeChatPayload(raw));
          break;

        case MessageType.GAME_EVENT: {
          const eventType = String(raw.eventType ?? raw.type ?? 'UNKNOWN');
          const data = (raw.data ?? {}) as Record<string, unknown>;
          opts.onGameEvent?.({ type: eventType, data });
          break;
        }

        case MessageType.ERROR:
          opts.onError?.(String(raw?.message || 'Unknown error'));
          break;
      }
    });

    client.connect();
  }, [options.userId]);

  const disconnect = useCallback(() => {
    clientRef.current?.disconnect();
    clientRef.current = null;
  }, []);

  const send = useCallback((type: string, payload: Record<string, unknown>) => {
    clientRef.current?.send(type, payload);
  }, []);

  const joinRoom = useCallback((roomId: string, seatId: number) => {
    send(MessageType.JOIN_ROOM, { roomId, seatId, userId: optionsRef.current.userId });
  }, [send]);

  const setReady = useCallback((roomId: string, seatId: number, ready: boolean) => {
    send(MessageType.READY, { roomId, seatId, ready });
  }, [send]);

  const sendGameAction = useCallback(
    (roomId: string, playerId: number, action: string, phase: string, target?: number, content?: string) => {
      send(MessageType.GAME_ACTION, {
        roomId,
        playerId,
        action,
        phase,
        target,
        content,
      });
    },
    [send]
  );

  const autoConnect = options.autoConnect !== false;

  useEffect(() => {
    if (!autoConnect) return;
    connect();
    return () => {
      disconnect();
    };
  }, [autoConnect, connect, disconnect]);

  return { connect, disconnect, send, joinRoom, setReady, sendGameAction, client: clientRef };
}

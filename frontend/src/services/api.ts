import { CreateRoomResponse, JoinRoomResponse, RoomSnapshotResponse } from '../types/game';
import { BOARD_TYPE_STANDARD } from '../constants/boardTypes';

const BASE_URL = '/api/room';

export interface CreateRoomOptions {
  roomId?: string;
  boardType?: string;
  aiCount?: number;
  hostUserId?: number;
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HTTP ${res.status}: ${text}`);
  }
  return res.json();
}

export const api = {
  createRoom(opts: CreateRoomOptions = {}): Promise<CreateRoomResponse> {
    const body = {
      boardType: opts.boardType ?? BOARD_TYPE_STANDARD,
      aiCount: opts.aiCount ?? 11,
      ...(opts.roomId ? { roomId: opts.roomId } : {}),
      ...(opts.hostUserId != null ? { hostUserId: opts.hostUserId } : {}),
    };
    return request<CreateRoomResponse>(BASE_URL, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  joinRoom(roomId: string, seatId: number, userId: number): Promise<JoinRoomResponse> {
    return request<JoinRoomResponse>(`${BASE_URL}/${roomId}/join`, {
      method: 'POST',
      body: JSON.stringify({ seatId, userId }),
    });
  },

  setReady(roomId: string, seatId: number, ready: boolean): Promise<JoinRoomResponse> {
    return request<JoinRoomResponse>(`${BASE_URL}/${roomId}/ready`, {
      method: 'POST',
      body: JSON.stringify({ seatId, ready }),
    });
  },

  startGame(
    roomId: string,
    userId?: number
  ): Promise<{
    success: boolean;
    phase?: string;
    message?: string;
    phaseSyncs?: Array<Record<string, unknown>>;
  }> {
    return request(`${BASE_URL}/${roomId}/start`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(userId != null ? { 'X-User-Id': String(userId) } : {}),
      },
      body: userId != null ? JSON.stringify({ userId }) : undefined,
    });
  },

  getRoom(roomId: string): Promise<RoomSnapshotResponse> {
    return request<RoomSnapshotResponse>(`${BASE_URL}/${roomId}`);
  },
};

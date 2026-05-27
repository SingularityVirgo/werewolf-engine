#!/usr/bin/env python3
"""ADR-007 M1: disconnect grace + reconnect smoke (requires running dev server)."""
from __future__ import annotations

import json
import os
import sys
import time

try:
    import requests
except ImportError:
    print("pip install -r scripts/requirements.txt", file=sys.stderr)
    sys.exit(1)

try:
    from websocket import create_connection
except ImportError:
    print("pip install -r scripts/requirements.txt", file=sys.stderr)
    sys.exit(1)

BASE = os.environ.get("WERWOLF_BASE_URL", "http://localhost:8080").rstrip("/")
WS_URL = os.environ.get("WERWOLF_WS_URL", "ws://localhost:8080/ws/game")
HOST_ID = int(os.environ.get("WERWOLF_HOST_USER_ID", "10001"))
RECONNECT_AFTER = float(os.environ.get("WERWOLF_RECONNECT_AFTER_SEC", "2"))


def post(path: str, body: dict | None = None) -> dict:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=30)
    r.raise_for_status()
    return r.json()


def recv_until(ws, msg_type: str, timeout: float = 15.0) -> dict:
    deadline = time.time() + timeout
    ws.settimeout(1.0)
    while time.time() < deadline:
        try:
            msg = json.loads(ws.recv())
            if msg.get("type") == msg_type:
                return msg
        except Exception:
            continue
    raise TimeoutError(f"timeout waiting for {msg_type}")


def main() -> int:
    token = str(HOST_ID)
    room = post("/api/room", {"hostUserId": HOST_ID, "aiCount": 11})
    room_id = room["roomId"]
    post(f"/api/room/{room_id}/ready", {"seatId": 1, "ready": True})

    ws = create_connection(f"{WS_URL}?token={token}", timeout=10)
    connected = recv_until(ws, "CONNECTED")
    user_id = connected.get("payload", {}).get("userId")
    print(f"[ok] CONNECTED userId={user_id}")

    ws.send(json.dumps({"type": "JOIN_ROOM", "payload": {"roomId": room_id, "seatId": 1}}))
    recv_until(ws, "JOIN_ROOM")
    post(f"/api/room/{room_id}/start", {"hostUserId": HOST_ID})
    recv_until(ws, "PHASE_SYNC")
    print("[ok] game started and PHASE_SYNC received")

    ws.close()
    print(f"[info] disconnected, waiting {RECONNECT_AFTER}s")
    time.sleep(RECONNECT_AFTER)

    ws2 = create_connection(f"{WS_URL}?token={token}", timeout=10)
    recv_until(ws2, "CONNECTED")
    rejoined = recv_until(ws2, "JOIN_ROOM", timeout=15)
    payload = rejoined.get("payload", {})
    ok = payload.get("reconnected") is True and payload.get("playerId") == 1
    print(f"[{'PASS' if ok else 'FAIL'}] reconnect reconnected={payload.get('reconnected')} playerId={payload.get('playerId')}")
    ws2.close()
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())

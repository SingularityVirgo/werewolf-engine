#!/usr/bin/env python3
"""ADR-007 §7.3: grace expiry (>30s) → AI_HOSTED; game continues without reconnect."""
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
HOST_ID = int(os.environ.get("WERWOLF_HOST_USER_ID", "10002"))
# Default 32s: grace TTL is 30s in application.properties
HOSTING_WAIT = float(os.environ.get("WERWOLF_HOSTING_WAIT_SEC", "32"))
MAX_TICKS = int(os.environ.get("WERWOLF_HOSTING_MAX_TICKS", "200"))


def post(path: str, body: dict | None = None) -> dict:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=60)
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


def recv_any(ws, timeout: float = 3.0) -> dict | None:
    deadline = time.time() + timeout
    ws.settimeout(0.5)
    while time.time() < deadline:
        try:
            return json.loads(ws.recv())
        except Exception:
            continue
    return None


def main() -> int:
    token = str(HOST_ID)
    room = post("/api/room", {"hostUserId": HOST_ID, "aiCount": 11})
    room_id = room["roomId"]
    post(f"/api/room/{room_id}/ready", {"seatId": 1, "ready": True})

    ws = create_connection(f"{WS_URL}?token={token}", timeout=10)
    recv_until(ws, "CONNECTED")
    ws.send(json.dumps({"type": "JOIN_ROOM", "payload": {"roomId": room_id, "seatId": 1}}))
    recv_until(ws, "JOIN_ROOM")
    post(f"/api/room/{room_id}/start", {"hostUserId": HOST_ID})
    recv_until(ws, "PHASE_SYNC")
    print(f"[ok] game started roomId={room_id}")

    ws.close()
    print(f"[info] disconnected, waiting {HOSTING_WAIT}s for grace expiry")
    time.sleep(HOSTING_WAIT)

    post(f"/api/room/{room_id}/phase-tick", {})
    print("[ok] phase-tick after grace window")

    ws2 = create_connection(f"{WS_URL}?token={token}", timeout=10)
    recv_until(ws2, "CONNECTED")
    stray = recv_any(ws2, timeout=3.0)
    ws2.close()
    no_reconnect = stray is None or stray.get("type") != "JOIN_ROOM"
    print(f"[{'PASS' if no_reconnect else 'FAIL'}] AI_HOSTED blocks auto-reconnect "
          f"(stray={stray.get('type') if stray else None})")

    game_over = False
    last = {}
    for i in range(MAX_TICKS):
        last = post(f"/api/room/{room_id}/phase-tick", {})
        if last.get("status") == "GAME_OVER":
            game_over = True
            print(f"[PASS] phase-tick to GAME_OVER ticks={i + 1}")
            break
        if last.get("status") == "STUCK":
            print(f"[FAIL] STUCK at tick {i + 1} phase={last.get('phase')}")
            break
    if not game_over:
        print(f"[FAIL] no GAME_OVER after {MAX_TICKS} ticks phase={last.get('phase')}")

    ok = no_reconnect and game_over
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())

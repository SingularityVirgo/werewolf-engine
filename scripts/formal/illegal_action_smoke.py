#!/usr/bin/env python3
"""Formal illegal GAME_ACTION smoke — ERROR must not mutate phase (PRD §1.2 P0)."""
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


def phase_from_sync(msg: dict) -> str | None:
    payload = msg.get("payload", {})
    sync = payload.get("phaseSync") or payload
    return sync.get("currentPhase")


def main() -> int:
    room = post("/api/room", {"hostUserId": HOST_ID, "aiCount": 11})
    room_id = room["roomId"]
    post(f"/api/room/{room_id}/ready", {"seatId": 1, "ready": True})

    ws = create_connection(f"{WS_URL}?token={HOST_ID}", timeout=10)
    recv_until(ws, "CONNECTED")
    ws.send(json.dumps({"type": "JOIN_ROOM", "payload": {"roomId": room_id, "seatId": 1}}))
    recv_until(ws, "JOIN_ROOM")

    post(f"/api/room/{room_id}/start", {"hostUserId": HOST_ID})

    phase_before = None
    deadline = time.time() + 15
    while time.time() < deadline:
        try:
            msg = json.loads(ws.recv())
        except Exception:
            continue
        if msg.get("type") == "PHASE_SYNC":
            phase = phase_from_sync(msg)
            if phase and phase != "WAITING":
                phase_before = phase
                break

    if not phase_before:
        print("[FAIL] no PHASE_SYNC after start")
        return 1

    ws.send(json.dumps({
        "type": "GAME_ACTION",
        "payload": {
            "roomId": room_id,
            "playerId": 1,
            "action": "VOTE",
            "phase": "DAY_VOTE",
            "target": 2,
        },
    }))
    time.sleep(0.3)
    got_error = False
    phase_after = phase_before
    for _ in range(10):
        try:
            msg = json.loads(ws.recv())
        except Exception:
            break
        if msg.get("type") == "ERROR":
            got_error = True
        if msg.get("type") == "ACTION_ACK":
            ack = msg.get("payload", {}).get("ack", {})
            if ack.get("success") is False:
                got_error = True
        if msg.get("type") == "PHASE_SYNC":
            p = phase_from_sync(msg)
            if p:
                phase_after = p

    ok = got_error and phase_after == phase_before
    print(f"[{'PASS' if ok else 'FAIL'}] illegal VOTE in {phase_before}: error={got_error} phase_unchanged={phase_after == phase_before}")
    ws.close()
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())

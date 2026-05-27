#!/usr/bin/env python3
"""ADR-007 §7.3: GAME_OVER后 room 相关 Redis 键应清空."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time

try:
    import requests
except ImportError:
    print("pip install -r scripts/requirements.txt", file=sys.stderr)
    sys.exit(1)

BASE = os.environ.get("WERWOLF_BASE_URL", "http://localhost:8080").rstrip("/")
HOST_ID = int(os.environ.get("WERWOLF_HOST_USER_ID", "10003"))
REDIS_CONTAINER = os.environ.get("WERWOLF_REDIS_CONTAINER", "werewolf-redis")
MAX_TICKS = int(os.environ.get("WERWOLF_REDIS_SMOKE_TICKS", "250"))


def post(path: str, body: dict | None = None) -> dict:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=60)
    r.raise_for_status()
    return r.json()


def redis_keys_for_room(room_id: str) -> list[str]:
    pattern = f"werewolf:*:{room_id}:*"
    cmd = ["docker", "exec", REDIS_CONTAINER, "redis-cli", "KEYS", pattern]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr or proc.stdout or "redis-cli failed")
    lines = [ln.strip() for ln in proc.stdout.splitlines() if ln.strip()]
    return lines


def main() -> int:
    room = post("/api/room", {"hostUserId": HOST_ID, "aiCount": 11})
    room_id = room["roomId"]
    post(f"/api/room/{room_id}/ready", {"seatId": 1, "ready": True})
    post(f"/api/room/{room_id}/start", {"hostUserId": HOST_ID})

    keys_before = redis_keys_for_room(room_id)
    print(f"[info] roomId={room_id} redis keys before GAME_OVER: {len(keys_before)}")
    if keys_before:
        print(f"       sample: {keys_before[:3]}")

    game_over = False
    for i in range(MAX_TICKS):
        tick = post(f"/api/room/{room_id}/phase-tick", {})
        if tick.get("status") == "GAME_OVER":
            game_over = True
            print(f"[ok] GAME_OVER at tick {i + 1}")
            break
        time.sleep(0.01)

    if not game_over:
        print(f"[FAIL] did not reach GAME_OVER in {MAX_TICKS} ticks")
        return 1

    time.sleep(0.5)
    keys_after = redis_keys_for_room(room_id)
    ok = len(keys_after) == 0
    print(f"[{'PASS' if ok else 'FAIL'}] redis room keys after GAME_OVER: {len(keys_after)}")
    if keys_after:
        for key in keys_after[:10]:
            print(f"       leftover: {key}")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())

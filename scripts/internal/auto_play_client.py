#!/usr/bin/env python3
"""
Internal 路径 A：一键 mock-auto-play 跑完整局（开发/状态机验证）。

路径: scripts/internal/auto_play_client.py
作用: POST /internal/game → start → mock-auto-play → 打印 action_log 条数。

用法: python scripts/internal/auto_play_client.py
"""

from __future__ import annotations

import os
import sys

import requests

BASE = os.environ.get("WERWOLF_BASE_URL", "http://localhost:8080").rstrip("/")
API = f"{BASE}/internal/game"


def main() -> int:
    room = requests.post(f"{API}/rooms", json={}, timeout=30).json()
    room_id = room["roomId"]
    print(f"roomId={room_id}")

    start = requests.post(f"{API}/rooms/{room_id}/start", timeout=30).json()
    if not start.get("success", True):
        print("start failed:", start, file=sys.stderr)
        return 1
    print(f"started phase={start.get('phase')}")

    result = requests.post(f"{API}/rooms/{room_id}/mock-auto-play", timeout=600).json()
    print("outcome:", result)

    log = requests.get(f"{API}/rooms/{room_id}/action-log", timeout=30).json()
    print(f"action_log entries={len(log.get('entries', []))}")

    if result.get("outcome") != "FINISHED":
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

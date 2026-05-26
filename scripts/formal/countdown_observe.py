#!/usr/bin/env python3
"""
P-05 验收：观察 WS PHASE_SYNC.countdown 在同阶段内递减（Formal 路径 B，约 35s）。

路径: scripts/formal/countdown_observe.py
作用: 订阅 phase/countdown 样本，断言 countdown 可见且递减；详见 gateway-integration.md §6。

用法: python scripts/formal/countdown_observe.py
"""

from __future__ import annotations

import json
import os
import sys
import time
from typing import Any

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
OBSERVE_SEC = float(os.environ.get("WERWOLF_COUNTDOWN_OBSERVE_SEC", "35"))


def post(path: str, body: dict | None = None) -> dict[str, Any]:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=30)
    r.raise_for_status()
    return r.json()


def main() -> int:
    print("=== PHASE_SYNC countdown observe ===\n")

    room = post("/api/room", {})
    room_id = room["roomId"]
    print(f"roomId={room_id}")

    for seat in range(1, 13):
        post(f"/api/room/{room_id}/ready", {"seatId": seat, "ready": True})

    ws = create_connection(WS_URL, timeout=10)
    _ = ws.recv()  # CONNECTED

    ws.send(json.dumps({
        "type": "JOIN_ROOM",
        "payload": {"roomId": room_id, "seatId": 1},
    }))
    time.sleep(0.2)

    start = post(f"/api/room/{room_id}/start")
    print(f"start: success={start.get('success')} phase={start.get('phase')}\n")
    print(f"{'time':>8}  {'phase':<22}  countdown  (auto phase-tick ~1.5s)\n")

    last_key: tuple[str, int | None] | None = None
    samples: list[tuple[str, int | None]] = []
    deadline = time.time() + OBSERVE_SEC

    ws.settimeout(1.0)
    while time.time() < deadline:
        try:
            raw = ws.recv()
        except Exception:
            continue
        msg = json.loads(raw)
        if msg.get("type") != "PHASE_SYNC":
            continue
        sync = msg.get("payload", {}).get("phaseSync", {})
        phase = sync.get("currentPhase", "?")
        countdown = sync.get("countdown")
        key = (phase, countdown)
        if key == last_key:
            continue
        last_key = key
        samples.append(key)
        ts = time.strftime("%H:%M:%S")
        cd = "—" if countdown is None else str(countdown)
        print(f"{ts:>8}  {phase:<22}  {cd:>9}")

    ws.close()

    phases_with_cd = {p for p, c in samples if c is not None}
    decrements = sum(
        1
        for i in range(1, len(samples))
        if samples[i][0] == samples[i - 1][0]
        and samples[i][1] is not None
        and samples[i - 1][1] is not None
        and samples[i][1] < samples[i - 1][1]
    )

    print(f"\n--- summary ---")
    print(f"distinct phase/countdown samples: {len(samples)}")
    print(f"phases with countdown: {sorted(phases_with_cd)}")
    print(f"same-phase countdown decreases: {decrements}")

    ok = len(phases_with_cd) > 0 and decrements > 0
    print(f"\n{'PASS' if ok else 'WARN'}: authoritative countdown "
          f"{'visible and decreasing' if ok else 'not clearly observed (check phase-tick-enabled)'}")
    return 0 if ok else 2


if __name__ == "__main__":
    sys.exit(main())

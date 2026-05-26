#!/usr/bin/env python3
"""
Formal 路径 B 全链路冒烟：HTTP 建房 + WS 订阅 + phase-tick 推进至 GAME_OVER。

路径: scripts/formal/formal_path_smoke.py
作用: 快速验证 Formal API/WS 闭环（8 项检查）；countdown 开启时末项可能 7/8，见 ADR-005 §14.1。

用法: python scripts/formal/formal_path_smoke.py
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
MAX_TICKS = int(os.environ.get("WERWOLF_SMOKE_TICKS", "200"))
PHASE_TICK_SLEEP = float(os.environ.get("WERWOLF_SMOKE_TICK_SLEEP", "0"))


def post(path: str, body: dict | None = None) -> dict[str, Any]:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=30)
    r.raise_for_status()
    return r.json()


def main() -> int:
    results: list[tuple[str, bool, str]] = []

    def record(name: str, ok: bool, detail: str = "") -> None:
        results.append((name, ok, detail))
        mark = "PASS" if ok else "FAIL"
        print(f"[{mark}] {name}" + (f" — {detail}" if detail else ""))

    print("=== Formal path B smoke ===\n")

    try:
        room = post("/api/room", {})
        room_id = room["roomId"]
        record("POST /api/room", bool(room_id), room_id)
    except Exception as e:
        record("POST /api/room", False, str(e))
        return 1

    try:
        for seat in range(1, 13):
            post(f"/api/room/{room_id}/ready", {"seatId": seat, "ready": True})
        record("join seat1 + ready x12", True)
    except Exception as e:
        record("join seat1 + ready x12", False, str(e))
        return 1

    ws_msgs: list[dict] = []
    try:
        ws = create_connection(WS_URL, timeout=10)
        raw = ws.recv()
        msg = json.loads(raw)
        ws_msgs.append(msg)
        record("WS CONNECTED", msg.get("type") == "CONNECTED", str(msg.get("type")))

        ws.send(json.dumps({
            "type": "JOIN_ROOM",
            "payload": {"roomId": room_id, "seatId": 1},
        }))
        time.sleep(0.3)
        while True:
            try:
                ws.settimeout(0.5)
                raw = ws.recv()
                ws_msgs.append(json.loads(raw))
            except Exception:
                break

        join_ok = any(m.get("type") == "JOIN_ROOM" for m in ws_msgs)
        push_ok = any(
            m.get("type") == "PHASE_SYNC"
            and m.get("payload", {}).get("seatId") == 1
            for m in ws_msgs
        )
        record("WS JOIN_ROOM ack", join_ok)
        record("WS push PHASE_SYNC after JOIN", push_ok, f"msgs={len(ws_msgs)}")
    except Exception as e:
        record("WS connect/JOIN", False, str(e))
        ws = None

    try:
        start = post(f"/api/room/{room_id}/start")
        ok = start.get("success") is True
        record("POST start", ok, str(start.get("phase")))
    except Exception as e:
        record("POST start", False, str(e))
        return 1

    if ws:
        time.sleep(0.5)
        try:
            while True:
                try:
                    ws.settimeout(0.5)
                    ws_msgs.append(json.loads(ws.recv()))
                except Exception:
                    break
            start_push = any(
                m.get("type") == "PHASE_SYNC"
                and m.get("payload", {}).get("phaseSync", {}).get("currentPhase")
                in ("NIGHT_WOLF", "NIGHT_START", "ROLE_ASSIGN")
                for m in ws_msgs
            )
            record("WS push PHASE_SYNC after start", start_push)
        except Exception as e:
            record("WS push after start", False, str(e))

    game_over = False
    try:
        for i in range(MAX_TICKS):
            tick = post(f"/api/room/{room_id}/phase-tick")
            status = tick.get("status")
            if i % 25 == 0:
                print(f"  tick {i}: {status} phase={tick.get('phase')}")
            if status == "GAME_OVER":
                game_over = True
                record("phase-tick to GAME_OVER", True, f"ticks={i}")
                break
            if status == "STUCK":
                record("phase-tick to GAME_OVER", False, f"STUCK at tick {i}")
                break
            if PHASE_TICK_SLEEP > 0:
                time.sleep(PHASE_TICK_SLEEP)
        if not game_over:
            snap = requests.get(f"{BASE}/api/room/{room_id}", timeout=10).json()
            record("phase-tick to GAME_OVER", False, f"max={MAX_TICKS} phase={snap.get('phase')}")
    except Exception as e:
        record("phase-tick loop", False, str(e))

    print("\n=== Summary ===")
    passed = sum(1 for _, ok, _ in results if ok)
    for name, ok, detail in results:
        mark = "OK" if ok else "X"
        print(f"  [{mark}] {name}" + (f" ({detail})" if detail and not ok else ""))
    print(f"\n{passed}/{len(results)} checks passed")
    return 0 if passed == len(results) else 2


if __name__ == "__main__":
    sys.exit(main())

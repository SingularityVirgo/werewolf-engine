#!/usr/bin/env python3
"""
Web UI 联调冒烟：单人建房（hostUserId + aiCount=11）→ WS 订阅 → 开局 → 断言推送类型。

用法: python scripts/formal/frontend_ws_smoke.py
依赖: pip install -r scripts/requirements.txt；后端 :8080 已启动。
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
LISTEN_SEC = float(os.environ.get("WERWOLF_UI_SMOKE_SEC", "45"))


def post(path: str, body: dict | None = None, headers: dict | None = None) -> dict[str, Any]:
    r = requests.post(f"{BASE}{path}", json=body or {}, headers=headers or {}, timeout=30)
    r.raise_for_status()
    return r.json()


def main() -> int:
    results: list[tuple[str, bool, str]] = []

    def record(name: str, ok: bool, detail: str = "") -> None:
        results.append((name, ok, detail))
        mark = "PASS" if ok else "FAIL"
        print(f"[{mark}] {name}" + (f" — {detail}" if detail else ""))

    host_user_id = 9001
    print("=== Frontend WS smoke (host + 11 AI) ===\n")

    try:
        room = post(
            "/api/room",
            {"boardType": "STANDARD_12_PRYH_IDIOT", "aiCount": 11, "hostUserId": host_user_id},
        )
        room_id = room["roomId"]
        record("POST /api/room (hostUserId)", bool(room_id), room_id)
        record("host auto seat #1", room.get("humanCount") == 1, f"humanCount={room.get('humanCount')}")
    except Exception as e:
        record("POST /api/room", False, str(e))
        return 1

    types_seen: set[str] = set()
    try:
        ws = create_connection(f"{WS_URL}?token={host_user_id}", timeout=10)
        connected = json.loads(ws.recv())
        types_seen.add(str(connected.get("type")))
        record("WS CONNECTED", connected.get("type") == "CONNECTED")

        ws.send(
            json.dumps(
                {
                    "type": "JOIN_ROOM",
                    "payload": {"roomId": room_id, "seatId": 1, "userId": host_user_id},
                }
            )
        )
        join_ack = json.loads(ws.recv())
        types_seen.add(str(join_ack.get("type")))
        record("WS JOIN_ROOM", join_ack.get("type") == "JOIN_ROOM")

        post(f"/api/room/{room_id}/ready", {"seatId": 1, "ready": True})
        start = post(
            f"/api/room/{room_id}/start",
            {"userId": host_user_id},
            headers={"X-User-Id": str(host_user_id)},
        )
        record("POST /start (host)", start.get("success") is True, str(start.get("phase")))

        deadline = time.time() + LISTEN_SEC
        ws.settimeout(1.0)
        while time.time() < deadline:
            try:
                raw = ws.recv()
            except Exception:
                continue
            if not raw:
                continue
            msg = json.loads(raw)
            t = str(msg.get("type"))
            types_seen.add(t)
            if t == "GAME_OVER":
                break

        ws.close()
    except Exception as e:
        record("WS session", False, str(e))
        return 1

    record("saw PHASE_SYNC", "PHASE_SYNC" in types_seen, ",".join(sorted(types_seen)))
    record(
        "saw GAME_EVENT or CHAT_BROADCAST",
        "GAME_EVENT" in types_seen or "CHAT_BROADCAST" in types_seen,
        ",".join(sorted(types_seen)),
    )
    record("saw GAME_OVER", "GAME_OVER" in types_seen, ",".join(sorted(types_seen)))

    passed = sum(1 for _, ok, _ in results if ok)
    total = len(results)
    print(f"\n=== {passed}/{total} passed ===")
    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())

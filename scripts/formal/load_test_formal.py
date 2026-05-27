#!/usr/bin/env python3
"""Formal path load test — N games via phase-tick to GAME_OVER (PRD §8.3)."""
from __future__ import annotations

import json
import os
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

try:
    import requests
except ImportError:
    print("pip install -r scripts/requirements.txt", file=sys.stderr)
    sys.exit(1)

BASE = os.environ.get("WERWOLF_BASE_URL", "http://localhost:8080").rstrip("/")
GAMES = int(os.environ.get("WERWOLF_LOAD_GAMES", "20"))
MAX_TICKS = int(os.environ.get("WERWOLF_LOAD_MAX_TICKS", "250"))
REPORT_DIR = Path(os.environ.get("WERWOLF_REPORT_DIR", "target/reports"))


def post(path: str, body: dict | None = None) -> dict:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=60)
    r.raise_for_status()
    return r.json()


def run_one_game(index: int) -> tuple[bool, str, int]:
    room = post("/api/room", {"hostUserId": 10001, "aiCount": 11})
    room_id = room["roomId"]
    post(f"/api/room/{room_id}/ready", {"seatId": 1, "ready": True})
    post(f"/api/room/{room_id}/start", {})
    ticks = 0
    last_phase = ""
    for _ in range(MAX_TICKS):
        tick = post(f"/api/room/{room_id}/phase-tick", {})
        ticks += 1
        last_phase = str(tick.get("phase", ""))
        if tick.get("status") == "GAME_OVER":
            return True, room_id, ticks
        time.sleep(0.01)
    return False, f"{room_id} stuck at {last_phase}", ticks


def main() -> int:
    print(f"=== Formal load test: {GAMES} games ===\n")
    results: list[dict] = []
    ok_count = 0
    t0 = time.time()
    for i in range(GAMES):
        ok, detail, ticks = run_one_game(i + 1)
        ok_count += int(ok)
        results.append({"game": i + 1, "ok": ok, "detail": detail, "ticks": ticks})
        mark = "OK" if ok else "FAIL"
        print(f"[{mark}] game {i + 1}/{GAMES} ticks={ticks} {detail}")

    elapsed = time.time() - t0
    summary = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "games": GAMES,
        "passed": ok_count,
        "failed": GAMES - ok_count,
        "success_rate": ok_count / GAMES if GAMES else 0,
        "elapsed_sec": round(elapsed, 2),
        "results": results,
    }
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    out = REPORT_DIR / f"load-test-{GAMES}games.json"
    out.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    md = REPORT_DIR / f"load-test-{GAMES}games.md"
    md.write_text(
        f"# Load test {GAMES} games\n\n"
        f"- Passed: {ok_count}/{GAMES}\n"
        f"- Success rate: {summary['success_rate']:.1%}\n"
        f"- Elapsed: {summary['elapsed_sec']}s\n"
        f"- JSON: `{out.name}`\n",
        encoding="utf-8",
    )
    print(f"\nReport: {out}")
    return 0 if ok_count == GAMES else 1


if __name__ == "__main__":
    raise SystemExit(main())

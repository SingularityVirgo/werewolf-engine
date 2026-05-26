#!/usr/bin/env python3
"""
Formal 路径 B + DeepSeek LLM 整局冒烟：phase-tick 至 GAME_OVER 并校验 action_log 含 LLM 记录。

路径: scripts/formal/formal_llm_smoke.py
作用: 验证 AI 启用环境下 Formal 全链路与 LLM 决策落盘；报告写入 scripts/reports/。

依赖: werewolf.ai.enabled=true、DEEPSEEK_API_KEY、pip install -r scripts/requirements.txt

用法: python scripts/formal/formal_llm_smoke.py

环境: WEREWOLF_BASE_URL, WEREWOLF_WS_URL, WEREWOLF_SMOKE_TICKS, WEREWOLF_TICK_TIMEOUT
"""
from __future__ import annotations

import json
import os
import sys
import time
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
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
INTERNAL = os.environ.get("WERWOLF_INTERNAL_BASE", f"{BASE}/internal/game").rstrip("/")
WS_URL = os.environ.get("WERWOLF_WS_URL", "ws://localhost:8080/ws/game")
MAX_TICKS = int(os.environ.get("WERWOLF_SMOKE_TICKS", "800"))
TICK_TIMEOUT = int(os.environ.get("WERWOLF_TICK_TIMEOUT", "120"))
REPORT_DIR = Path(__file__).resolve().parent.parent / "reports"


def post_formal(path: str, body: dict | None = None, timeout: int = 30) -> dict[str, Any]:
    r = requests.post(f"{BASE}{path}", json=body or {}, timeout=timeout)
    r.raise_for_status()
    return r.json()


def get_internal(path: str, timeout: int = 30) -> dict[str, Any]:
    r = requests.get(f"{INTERNAL}{path}", timeout=timeout)
    r.raise_for_status()
    return r.json()


def analyze_action_log(entries: list[dict]) -> dict[str, Any]:
    player_actions = [e for e in entries if e.get("action")]
    with_thinking = [e for e in player_actions if e.get("thinking")]
    llm_actions = [e for e in player_actions if e.get("modelId")]
    mock_like = [
        e for e in player_actions
        if (e.get("modelId") or "").lower() == "mock"
        or (e.get("reason") or "").lower().startswith("mock")
    ]
    models = Counter(e.get("modelId") or "none" for e in player_actions)
    phases = Counter(e.get("phase") for e in player_actions)
    actions_by_type = Counter(e.get("action") for e in player_actions)
    return {
        "totalEntries": len(entries),
        "playerActions": len(player_actions),
        "withThinking": len(with_thinking),
        "withModelId": len(llm_actions),
        "mockFallbackLike": len(mock_like),
        "llmLikeActions": len(llm_actions),
        "modelIds": dict(models),
        "actionsByPhase": dict(phases),
        "actionsByType": dict(actions_by_type),
        "sampleThinking": [
            {
                "playerId": e.get("playerId"),
                "action": e.get("action"),
                "phase": e.get("phase"),
                "modelId": e.get("modelId"),
                "thinkingPreview": (e.get("thinking") or "")[:120],
            }
            for e in with_thinking[:5]
        ],
    }


def run() -> dict[str, Any]:
    t0 = time.perf_counter()
    started_at = datetime.now(timezone.utc).isoformat()

    report: dict[str, Any] = {
        "path": "B_formal_LLM",
        "startedAt": started_at,
        "baseUrl": BASE,
        "maxTicks": MAX_TICKS,
        "tickTimeoutSec": TICK_TIMEOUT,
        "checks": [],
    }

    def check(name: str, ok: bool, detail: str = "") -> None:
        report["checks"].append({"name": name, "pass": ok, "detail": detail})

    room_id = post_formal("/api/room", {})["roomId"]
    report["roomId"] = room_id
    check("create_room", True, room_id)

    for seat in range(1, 13):
        post_formal(f"/api/room/{room_id}/ready", {"seatId": seat, "ready": True})
    check("ready_x12", True)

    ws_msgs: list[dict] = []
    ws = create_connection(WS_URL, timeout=15)
    ws.recv()
    ws.send(json.dumps({"type": "JOIN_ROOM", "payload": {"roomId": room_id, "seatId": 1}}))
    time.sleep(0.5)
    ws.settimeout(0.5)
    while True:
        try:
            ws_msgs.append(json.loads(ws.recv()))
        except Exception:
            break
    ws.close()

    join_push = any(
        m.get("type") == "PHASE_SYNC" and m.get("payload", {}).get("seatId") == 1
        for m in ws_msgs
    )
    check("ws_join_push", join_push, f"wsMsgs={len(ws_msgs)}")

    start = post_formal(f"/api/room/{room_id}/start")
    check("start", start.get("success") is True, str(start.get("phase")))
    report["startPhase"] = start.get("phase")

    status_hist: Counter[str] = Counter()
    phase_samples: list[dict] = []
    tick_latencies_ms: list[float] = []
    game_over_tick: int | None = None
    stuck_ticks = 0

    for i in range(MAX_TICKS):
        t_tick = time.perf_counter()
        try:
            tick = post_formal(
                f"/api/room/{room_id}/phase-tick",
                timeout=TICK_TIMEOUT,
            )
        except requests.exceptions.Timeout:
            status_hist["HTTP_TIMEOUT"] += 1
            stuck_ticks += 1
            continue
        latency = (time.perf_counter() - t_tick) * 1000
        tick_latencies_ms.append(latency)

        status = tick.get("status", "UNKNOWN")
        phase = tick.get("phase", "")
        status_hist[status] += 1

        if i % 20 == 0 or status in ("GAME_OVER", "STUCK"):
            phase_samples.append({"tick": i, "status": status, "phase": phase, "ms": round(latency, 1)})

        if status == "STUCK":
            stuck_ticks += 1
        if status == "GAME_OVER":
            game_over_tick = i
            break

    duration_sec = round(time.perf_counter() - t0, 2)
    snap = requests.get(f"{BASE}/api/room/{room_id}", timeout=30).json()

    report["timing"] = {
        "durationSec": duration_sec,
        "ticksExecuted": len(tick_latencies_ms),
        "gameOverTick": game_over_tick,
        "stuckTicks": stuck_ticks,
        "avgTickMs": round(sum(tick_latencies_ms) / len(tick_latencies_ms), 1) if tick_latencies_ms else 0,
        "maxTickMs": round(max(tick_latencies_ms), 1) if tick_latencies_ms else 0,
        "p95TickMs": round(sorted(tick_latencies_ms)[int(len(tick_latencies_ms) * 0.95)] if tick_latencies_ms else 0, 1),
    }
    report["tickStatusHistogram"] = dict(status_hist)
    report["phaseSamples"] = phase_samples
    report["finalSnapshot"] = snap
    report["success"] = game_over_tick is not None and snap.get("phase") == "GAME_OVER"

    check("game_over", report["success"], f"tick={game_over_tick} phase={snap.get('phase')}")

    try:
        log = get_internal(f"/rooms/{room_id}/action-log")
        entries = log.get("entries") or []
        report["actionLog"] = analyze_action_log(entries)
        llm_like = report["actionLog"]["withModelId"]
        check("action_log_llm", llm_like > 0, f"modelId={llm_like} thinking={report['actionLog']['withThinking']}")
    except Exception as e:
        report["actionLogError"] = str(e)
        check("action_log", False, str(e))

    report["finishedAt"] = datetime.now(timezone.utc).isoformat()
    report["checksPassed"] = sum(1 for c in report["checks"] if c["pass"])
    report["checksTotal"] = len(report["checks"])
    return report


def main() -> int:
    print("=== Formal B + LLM smoke ===\n")
    try:
        report = run()
    except Exception as e:
        print(f"FATAL: {e}", file=sys.stderr)
        return 1

    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    out = REPORT_DIR / f"formal-llm-smoke-{ts}.json"
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print("\n--- Summary ---")
    print(f"roomId:        {report.get('roomId')}")
    print(f"success:       {report.get('success')}")
    print(f"durationSec:   {report['timing']['durationSec']}")
    print(f"gameOverTick:  {report['timing']['gameOverTick']}")
    print(f"avgTickMs:     {report['timing']['avgTickMs']}")
    print(f"maxTickMs:     {report['timing']['maxTickMs']}")
    print(f"tick histogram:{report['tickStatusHistogram']}")
    if "actionLog" in report:
        al = report["actionLog"]
        print(f"playerActions: {al['playerActions']}  llmModelId={al['withModelId']}  thinking={al['withThinking']}")
        print(f"modelIds:      {al['modelIds']}")
        print(f"actionTypes:   {al.get('actionsByType', {})}")
    print(f"checks:        {report['checksPassed']}/{report['checksTotal']}")
    print(f"report file:   {out}")

    return 0 if report.get("success") and report["checksPassed"] == report["checksTotal"] else 2


if __name__ == "__main__":
    sys.exit(main())

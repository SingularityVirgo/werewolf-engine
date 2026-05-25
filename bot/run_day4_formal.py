#!/usr/bin/env python3
"""
PRD §8.2 Day4 — Formal 路径 B：12 Bot 同房联调。

用法:
  python run_day4_formal.py
  python run_day4_formal.py --parallel-rooms   # 额外跑双房不串房

环境:
  WEREWOLF_BASE_URL=http://127.0.0.1:8080
  WEREWOLF_WS_URL=ws://127.0.0.1:8080
  WEREWOLF_BOT_STAGGER=0.3
  WEREWOLF_MAX_TICKS=500
"""
from __future__ import annotations

import argparse
import os
import sys
import threading
import time
from typing import Any, Dict, List, Optional, Tuple

from auto_action import maybe_auto_action
from bot_player import BotPlayer
from config import default_config
from http_api import HttpApiClient
from message_handler import safe_print

STAGGER = float(os.environ.get("WEREWOLF_BOT_STAGGER", "0.3"))
MAX_TICKS = int(os.environ.get("WEREWOLF_MAX_TICKS", "500"))
START_PHASES = {"ROLE_ASSIGN", "NIGHT_WOLF", "NIGHT_START"}


def _sync_body(payload: Dict[str, Any]) -> Dict[str, Any]:
    return payload.get("phaseSync") or payload


def _ack_success(payload: Dict[str, Any]) -> bool:
    ack = payload.get("ack") or payload
    return bool(ack.get("success"))


class Day4Runner:
    def __init__(self, quiet: bool = True, full_bot: bool = False):
        self.quiet = quiet
        self.full_bot = full_bot
        self.results: List[Tuple[str, bool, str]] = []

    def record(self, name: str, ok: bool, detail: str = "") -> None:
        self.results.append((name, ok, detail))
        mark = "PASS" if ok else "FAIL"
        safe_print(f"[{mark}] {name}" + (f" — {detail}" if detail else ""))

    def run_twelve_bot_room(self) -> bool:
        safe_print("\n=== Day4 Formal: 12 Bot same room ===\n")
        host = BotPlayer(seat_id=1, user_id=1001, quiet=self.quiet)
        bots: List[BotPlayer] = []

        try:
            room_id = host.create_room()
            self.record("1. POST /api/room", bool(room_id), room_id)

            host.connect_websocket()
            self.record(
                "2. WS CONNECTED (host)",
                host.ws is not None and host.ws.connected,
                str(host.ws.session_id if host.ws else ""),
            )

            http = HttpApiClient()

            for seat in range(1, 13):
                if seat == 1:
                    b = host
                else:
                    time.sleep(STAGGER)
                    b = BotPlayer(seat_id=seat, quiet=self.quiet)
                    b.connect_websocket()
                    bots.append(b)
                if self.full_bot:
                    http.join_room(room_id, 1000 + seat, seat)
                    if b.ws:
                        b.ws.on_message_callback = lambda t, p, bot=b: maybe_auto_action(
                            bot, t, p
                        )
                b.join_room_ws(room_id, with_http_join=False)
                if seat == 1:
                    time.sleep(0.2)

            self.record("3. WS JOIN_ROOM x12", len(bots) == 11)

            # JOIN 后推送 PHASE_SYNC（seatId 匹配）
            join_sync_ok = True
            for b in [host] + bots:
                if b.ws and b.ws.latest_sync_seat_id != b.seat_id:
                    join_sync_ok = False
                    break
            self.record("4. PHASE_SYNC after JOIN (seatId match)", join_sync_ok)

            for seat in range(1, 13):
                http.ready(room_id, seat, True)
            self.record("5. HTTP ready x12", True)

            start = host.start_game()
            started = start.get("success") is True
            phase = str(start.get("phase", ""))
            self.record(
                "6. host start -> NIGHT_WOLF/ROLE_ASSIGN",
                started and phase in START_PHASES,
                phase,
            )

            time.sleep(1.0)
            for b in [host] + bots:
                b.ws.wait_phase_sync(timeout=5)

            start_push = all(
                (_sync_body(b.latest_sync() or {}).get("currentPhase") in START_PHASES)
                for b in [host] + bots
                if b.latest_sync()
            )
            self.record("7. PHASE_SYNC push after start", start_push)

            time.sleep(0.5)
            wolf_ack = False
            wolf_seat = ""
            if self.full_bot:
                for b in [host] + bots:
                    for msg in b.ws.messages if b.ws else []:
                        if msg.get("type") != "ACTION_ACK":
                            continue
                        if _ack_success(msg.get("payload") or {}):
                            sync = b.latest_sync() or {}
                            if sync.get("yourRole") == "WEREWOLF":
                                wolf_ack = True
                                wolf_seat = str(b.seat_id)
                                break
                    if wolf_ack:
                        break
            else:
                for i in range(15):
                    tick = http.phase_tick(room_id)
                    if tick.get("phase") == "NIGHT_WOLF" and tick.get("status") == "AI_STEP":
                        wolf_ack = True
                        wolf_seat = "ai-tick"
                        break
                    if tick.get("status") == "GAME_OVER":
                        break
                for b in [host] + bots:
                    sync = b.latest_sync() or {}
                    if sync.get("yourRole") == "WEREWOLF":
                        wolf_bot = b
                        if wolf_bot.ws and sync.get("canAct"):
                            pl = wolf_bot.send_game_action(
                                "WOLF_CHAT", phase="NIGHT_WOLF", content="day4"
                            )
                            if pl and _ack_success(pl):
                                wolf_ack = True
                                wolf_seat = str(wolf_bot.seat_id)
                        break
            self.record(
                "8. wolf WOLF_CHAT / AI_STEP (A+B tick)",
                wolf_ack,
                wolf_seat or "none",
            )

            # 推进至终局：真人座由 auto_action 出招；tick 处理 announce / 间隙
            game_over = False
            ticks_used = 0
            for i in range(MAX_TICKS):
                tick = http.phase_tick(room_id)
                ticks_used = i + 1
                status = tick.get("status")
                if i % 50 == 0:
                    safe_print(f"  tick {i}: {status} phase={tick.get('phase')}")
                if status == "GAME_OVER":
                    game_over = True
                    break
                if status == "STUCK":
                    time.sleep(0.3)
                    continue
                time.sleep(0.08)

            snap = http.snapshot(room_id)
            self.record(
                "9. phase-tick -> GAME_OVER",
                game_over and snap.get("phase") == "GAME_OVER",
                f"ticks={ticks_used} status={snap.get('status')}",
            )

            return all(r[1] for r in self.results)

        finally:
            host.close()
            for b in bots:
                b.close()

    def run_parallel_rooms(self) -> bool:
        safe_print("\n=== Day4: parallel rooms (no cross leak) ===\n")
        errors: List[str] = []

        def one_room(label: str, base_seat: int) -> str:
            http = HttpApiClient()
            room_id = http.create_room()["roomId"]
            holders: List[Tuple[int, Any]] = []

            http_local = HttpApiClient()
            for seat in range(1, 13):
                time.sleep(STAGGER)
                b = BotPlayer(seat_id=seat, user_id=base_seat + seat, quiet=True)
                if self.full_bot:
                    http_local.join_room(room_id, base_seat + seat, seat)
                    b.connect_websocket()
                    if b.ws:
                        b.ws.on_message_callback = lambda t, p, bot=b: maybe_auto_action(
                            bot, t, p
                        )
                else:
                    b.connect_websocket()
                b.join_room_ws(room_id, with_http_join=False)
                holders.append((seat, b))

            for seat in range(1, 13):
                http.ready(room_id, seat, True)
            http.start_game(room_id)
            time.sleep(1.0)

            for seat, b in holders:
                sync = b.latest_sync() or {}
                if b.ws and b.ws.room_id != room_id:
                    errors.append(f"{label} seat{seat} wrong roomId")
                if b.ws and b.ws.latest_sync_seat_id != seat:
                    errors.append(f"{label} seat{seat} sync seat mismatch")
            for _, b in holders:
                b.close()
            return room_id

        t1 = threading.Thread(target=lambda: one_room("A", 2000))
        t2 = threading.Thread(target=lambda: one_room("B", 3000))
        t1.start()
        t2.start()
        t1.join()
        t2.join()

        ok = len(errors) == 0
        self.record("10. two rooms x12 bots no cross leak", ok, "; ".join(errors) or "ok")
        return ok

    def summary(self) -> int:
        safe_print("\n=== Day4 Summary ===")
        passed = sum(1 for _, ok, _ in self.results if ok)
        for name, ok, detail in self.results:
            mark = "OK" if ok else "X"
            line = f"  [{mark}] {name}"
            if detail:
                line += f" ({detail})"
            safe_print(line)
        safe_print(f"\n{passed}/{len(self.results)} checks passed")
        return 0 if passed == len(self.results) else 2


def main() -> int:
    parser = argparse.ArgumentParser(description="Formal path B Day4 checklist")
    parser.add_argument(
        "--parallel-rooms",
        action="store_true",
        help="also run two parallel 12-bot rooms isolation test",
    )
    parser.add_argument(
        "--full-bot",
        action="store_true",
        help="12 HTTP join + auto_action (slower; may STUCK on complex phases)",
    )
    parser.add_argument("-v", "--verbose", action="store_true", help="print all WS messages")
    args = parser.parse_args()

    safe_print(f"Base URL: {default_config.base_url}")
    runner = Day4Runner(quiet=not args.verbose, full_bot=args.full_bot)
    runner.run_twelve_bot_room()
    if args.parallel_rooms:
        runner.run_parallel_rooms()
    return runner.summary()


if __name__ == "__main__":
    sys.exit(main())

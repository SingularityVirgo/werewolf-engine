"""
Minimal auto-play for Formal 12-bot rooms (Mock-friendly).
When seat is human (HTTP join), react to PHASE_SYNC.canAct via GAME_ACTION.
"""
from __future__ import annotations

import random
from typing import Any, Dict

from .bot_player import BotPlayer


def _sync(payload: Dict[str, Any]) -> Dict[str, Any]:
    return payload.get("phaseSync") or payload


def maybe_auto_action(bot: BotPlayer, msg_type: str, payload: Dict[str, Any]) -> bool:
    """Return True if an action was sent."""
    if msg_type != "PHASE_SYNC" or not bot.room_id:
        return False

    sync = _sync(payload)
    if not sync.get("canAct"):
        return False

    phase = sync.get("currentPhase")
    role = sync.get("yourRole")
    alive = sync.get("alivePlayers") or []

    try:
        if phase == "NIGHT_WOLF" and role == "WEREWOLF":
            if not sync.get("wolfChatInPhase"):
                bot.send_game_action("WOLF_CHAT", phase=phase, content="day4-bot")
                return True
            targets = [p for p in alive if p != bot.seat_id]
            if targets:
                bot.send_game_action("KILL", phase=phase, target=random.choice(targets))
                return True

        if phase == "NIGHT_SEER" and role == "SEER":
            targets = [p for p in alive if p != bot.seat_id]
            if targets:
                bot.send_game_action("CHECK", phase=phase, target=random.choice(targets))
                return True

        if phase == "NIGHT_WITCH" and role == "WITCH":
            bot.send_game_action("SKIP", phase=phase)
            return True

        if phase in ("DAY_DISCUSS", "LAST_WORDS"):
            bot.send_game_action("SKIP_SPEAK", phase=phase)
            return True

        if phase == "DAY_VOTE":
            targets = [p for p in alive if p != bot.seat_id]
            if targets:
                bot.send_game_action("VOTE", phase=phase, target=random.choice(targets))
            else:
                bot.send_game_action("SKIP_VOTE", phase=phase)
            return True

        if phase == "HUNTER_SHOOT":
            bot.send_game_action("SKIP", phase=phase)
            return True

        bot.send_game_action("SKIP", phase=phase)
        return True
    except Exception:
        return False

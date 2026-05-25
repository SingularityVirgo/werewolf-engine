"""
Bot 玩家 — Formal 路径 B（HTTP + WS）
"""
import time
from typing import Any, Dict, Optional

from config import BotConfig, default_config
from http_api import HttpApiClient
from message_handler import safe_print
from ws_client import WebSocketClient


class BotPlayer:
    """单座 Bot：HTTP 占座/准备 + WS 绑定与动作"""

    def __init__(
        self,
        seat_id: int,
        user_id: Optional[int] = None,
        config: BotConfig = default_config,
        quiet: bool = True,
    ):
        self.seat_id = seat_id
        self.user_id = user_id if user_id is not None else 1000 + seat_id
        self.config = config
        self.token = config.get_token(self.user_id)
        self.bot_id = f"Bot-{seat_id}"
        self.quiet = quiet

        self.http = HttpApiClient(config)
        self.ws: Optional[WebSocketClient] = None
        self.room_id: Optional[str] = None
        self.is_host = False

    def create_room(self) -> str:
        safe_print(f"[{self.bot_id}] create room")
        resp = self.http.create_room()
        self.room_id = resp["roomId"]
        self.is_host = True
        return self.room_id

    def connect_websocket(self):
        self.ws = WebSocketClient(self.token, self.config, self.bot_id, quiet=self.quiet)
        self.ws.connect()

    def join_room_http(self, room_id: str):
        """HTTP 占座（真人座）；AI 驱动局勿对全部座位调用。"""
        self.room_id = room_id
        resp = self.http.join_room(room_id, self.user_id, self.seat_id)
        safe_print(f"[{self.bot_id}] http join seatId={resp.get('seatId')}")

    def join_room_ws(self, room_id: str, with_http_join: bool = False):
        if not self.ws:
            raise RuntimeError("connect websocket first")
        self.room_id = room_id
        if with_http_join:
            self.join_room_http(room_id)
        self.ws.join_room(room_id, self.seat_id, self.user_id if with_http_join else None)
        sync = self.ws.wait_phase_sync(timeout=8)
        return sync

    def ready_http(self, is_ready: bool = True):
        if not self.room_id:
            raise RuntimeError("no room")
        self.http.ready(self.room_id, self.seat_id, is_ready)

    def ready_ws(self, is_ready: bool = True):
        if not self.ws or not self.room_id:
            raise RuntimeError("no ws room")
        self.ws.ready(self.room_id, self.seat_id, is_ready)
        time.sleep(0.2)

    def start_game(self) -> Dict[str, Any]:
        if not self.is_host or not self.room_id:
            raise RuntimeError("host only")
        return self.http.start_game(self.room_id)

    def send_game_action(
        self,
        action: str,
        phase: Optional[str] = None,
        target: Optional[int] = None,
        content: Optional[str] = None,
    ) -> Optional[Dict[str, Any]]:
        if not self.ws or not self.room_id:
            raise RuntimeError("no ws")
        phase = phase or (self.ws.latest_phase_sync or {}).get("currentPhase")
        if not phase:
            raise RuntimeError("no phase")
        self.ws.send_game_action(
            self.room_id,
            self.seat_id,
            action,
            phase,
            target,
            content,
        )
        return self.ws.wait_action_ack(timeout=10)

    def latest_sync(self) -> Optional[Dict[str, Any]]:
        return self.ws.latest_phase_sync if self.ws else None

    def close(self):
        if self.ws:
            self.ws.close()

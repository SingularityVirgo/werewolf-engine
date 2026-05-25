"""
WebSocket 客户端 — Formal 路径 B（/ws/game）
"""
import json
import threading
import time
import uuid
from typing import Any, Callable, Dict, Optional

import websocket

from config import BotConfig, default_config
from message_handler import MessageHandler, safe_print


class WebSocketClient:
    WS_PATH = "/ws/game"

    def __init__(
        self,
        token: str,
        config: BotConfig = default_config,
        bot_id: str = "Bot",
        quiet: bool = False,
    ):
        self.token = token
        self.config = config
        self.bot_id = bot_id
        self.quiet = quiet
        self.ws: Optional[websocket.WebSocket] = None
        self.message_handler = MessageHandler(bot_id, quiet=quiet)
        self.connected = False
        self.session_id: Optional[str] = None
        self.seat_id: Optional[int] = None
        self.room_id: Optional[str] = None
        self.latest_phase_sync: Optional[Dict[str, Any]] = None
        self.latest_sync_seat_id: Optional[int] = None
        self.messages: list[Dict[str, Any]] = []

        self.on_message_callback: Optional[Callable] = None
        self._recv_thread: Optional[threading.Thread] = None
        self._running = False
        self._connected_event = threading.Event()
        self._sync_event = threading.Event()
        self._ack_event = threading.Event()
        self._last_ack: Optional[Dict[str, Any]] = None
        self._lock = threading.Lock()

    def _build_url(self) -> str:
        host_part = self.config.ws_url
        if not host_part.startswith("ws://") and not host_part.startswith("wss://"):
            host_part = f"ws://{host_part}"
        return f"{host_part}{self.WS_PATH}?token={self.token}"

    def connect(self):
        ws_url = self._build_url()
        if not self.quiet:
            safe_print(f"[{self.bot_id}] connecting {ws_url}")

        self._connected_event = threading.Event()
        self._running = True
        self.ws = websocket.WebSocket()
        self.ws.connect(ws_url, timeout=10)
        self.connected = True

        self._recv_thread = threading.Thread(
            target=self._recv_loop,
            name=f"ws-recv-{self.bot_id}",
            daemon=True,
        )
        self._recv_thread.start()

        if not self._connected_event.wait(timeout=10):
            raise TimeoutError(f"[{self.bot_id}] CONNECTED timeout")

    def _recv_loop(self):
        try:
            self.ws.settimeout(1.0)
        except Exception:
            pass

        while self._running and self.ws:
            try:
                raw = self.ws.recv()
                if not raw:
                    break
                self._on_message(raw)
            except websocket.WebSocketTimeoutException:
                continue
            except websocket.WebSocketConnectionClosedException:
                break
            except Exception as e:
                if self._running and not self.quiet:
                    safe_print(f"[{self.bot_id}] recv error: {e}")
                break
        self.connected = False

    def _on_message(self, raw_message: str):
        if not self.quiet:
            self.message_handler.handle_message(raw_message)

        try:
            data = json.loads(raw_message)
        except json.JSONDecodeError:
            return

        with self._lock:
            self.messages.append(data)

        msg_type = data.get("type")
        payload = data.get("payload") or {}

        if msg_type == "CONNECTED":
            self.session_id = payload.get("sessionId")
            self._connected_event.set()

        elif msg_type == "PHASE_SYNC":
            self.latest_sync_seat_id = payload.get("seatId")
            sync = payload.get("phaseSync") or payload
            self.latest_phase_sync = sync
            self._sync_event.set()

        elif msg_type == "ACTION_ACK":
            self._last_ack = payload
            self._ack_event.set()

        if self.on_message_callback:
            self.on_message_callback(msg_type, payload)

    def send_message(self, msg_type: str, payload: Dict[str, Any], request_id: Optional[str] = None):
        if not self.connected or not self.ws:
            raise RuntimeError(f"[{self.bot_id}] not connected")

        message = {
            "type": msg_type,
            "payload": payload,
            "timestamp": int(time.time() * 1000),
            "requestId": request_id or str(uuid.uuid4()),
        }
        self.ws.send(json.dumps(message, ensure_ascii=False))

    def join_room(self, room_id: str, seat_id: int, user_id: Optional[int] = None):
        self.room_id = room_id
        self.seat_id = seat_id
        payload: Dict[str, Any] = {"roomId": room_id, "seatId": seat_id}
        if user_id is not None:
            payload["userId"] = user_id
        self._sync_event.clear()
        self.send_message("JOIN_ROOM", payload)

    def ready(self, room_id: str, seat_id: int, is_ready: bool = True):
        self.send_message(
            "READY",
            {"roomId": room_id, "seatId": seat_id, "ready": is_ready},
        )

    def send_game_action(
        self,
        room_id: str,
        player_id: int,
        action: str,
        phase: str,
        target: Optional[int] = None,
        content: Optional[str] = None,
    ):
        self._ack_event.clear()
        payload: Dict[str, Any] = {
            "roomId": room_id,
            "playerId": player_id,
            "action": action,
            "phase": phase,
        }
        if target is not None:
            payload["target"] = target
        if content:
            payload["content"] = content
        self.send_message("GAME_ACTION", payload)

    def wait_phase_sync(self, timeout: float = 10.0) -> Optional[Dict[str, Any]]:
        if self._sync_event.wait(timeout):
            return self.latest_phase_sync
        return None

    def wait_action_ack(self, timeout: float = 10.0) -> Optional[Dict[str, Any]]:
        if self._ack_event.wait(timeout):
            return self._last_ack
        return None

    def close(self):
        self._running = False
        if self.ws and self.connected:
            try:
                self.ws.close()
            except Exception:
                pass
        self.connected = False

"""
HTTP API — Formal 路径 B（/api/room）
"""
import requests
from typing import Any, Dict, Optional

from config import BotConfig, default_config


class HttpApiClient:
    def __init__(self, config: BotConfig = default_config):
        self.config = config
        self.session = requests.Session()

    def create_room(
        self,
        room_id: Optional[str] = None,
        *,
        ai_count: int = 0,
        host_user_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        url = self.config.get_http_url("/api/room")
        body: Dict[str, Any] = {"aiCount": ai_count}
        if room_id:
            body["roomId"] = room_id
        if host_user_id is not None:
            body["hostUserId"] = host_user_id
        response = self.session.post(url, json=body, timeout=30)
        response.raise_for_status()
        return response.json()

    def join_room(
        self,
        room_id: str,
        user_id: int,
        seat_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        url = self.config.get_http_url(f"/api/room/{room_id}/join")
        body: Dict[str, Any] = {"userId": user_id}
        if seat_id is not None:
            body["seatId"] = seat_id
        response = self.session.post(
            url,
            json=body,
            timeout=30,
        )
        response.raise_for_status()
        return response.json()

    def ready(self, room_id: str, seat_id: int, is_ready: bool) -> Dict[str, Any]:
        url = self.config.get_http_url(f"/api/room/{room_id}/ready")
        response = self.session.post(
            url,
            json={"seatId": seat_id, "ready": is_ready},
            timeout=30,
        )
        response.raise_for_status()
        return response.json()

    def start_game(self, room_id: str) -> Dict[str, Any]:
        url = self.config.get_http_url(f"/api/room/{room_id}/start")
        response = self.session.post(url, timeout=30)
        response.raise_for_status()
        return response.json()

    def phase_tick(self, room_id: str) -> Dict[str, Any]:
        url = self.config.get_http_url(f"/api/room/{room_id}/phase-tick")
        response = self.session.post(url, timeout=60)
        response.raise_for_status()
        return response.json()

    def snapshot(self, room_id: str) -> Dict[str, Any]:
        url = self.config.get_http_url(f"/api/room/{room_id}")
        response = self.session.get(url, timeout=30)
        response.raise_for_status()
        return response.json()

    def leave_room(self, room_id: str, seat_id: int, user_id: int) -> Dict[str, Any]:
        url = self.config.get_http_url(f"/api/room/{room_id}/leave")
        response = self.session.post(
            url,
            json={"seatId": seat_id, "userId": user_id},
            timeout=30,
        )
        response.raise_for_status()
        return response.json()

    def dissolve_room(self, room_id: str, user_id: Optional[int] = None) -> None:
        url = self.config.get_http_url(f"/api/room/{room_id}")
        headers = {}
        body = {}
        if user_id is not None:
            body["userId"] = user_id
            headers["X-User-Id"] = str(user_id)
        response = self.session.delete(url, json=body or None, headers=headers, timeout=30)
        response.raise_for_status()

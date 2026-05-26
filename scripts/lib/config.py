"""
配置模块 - Formal 路径 B
默认端口与 Spring Boot application.properties 一致（8080）
"""
import os


class BotConfig:
    """Bot配置类"""

    def __init__(
        self,
        base_url: str = "http://localhost:8080",
        ws_url: str = "ws://localhost:8080",
    ):
        self.base_url = os.getenv("WEREWOLF_BASE_URL", base_url).rstrip("/")
        self.ws_url = os.getenv("WEREWOLF_WS_URL", ws_url).rstrip("/")

    def get_token(self, user_id: int) -> str:
        """MVP：userId 即 token（PRD §4.2.4 目标；当前网关未解析）"""
        return str(user_id)

    def get_http_url(self, path: str) -> str:
        return f"{self.base_url}{path}"


default_config = BotConfig()

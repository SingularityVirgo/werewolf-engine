# 联调与验收脚本

Python 联调、冒烟与压测工具，合并原 `bot/` 与根级 `scripts/*.py`。

**依赖**：Python 3.11+，`pip install -r scripts/requirements.txt`

**环境变量**（可选）：

```powershell
$env:WEREWOLF_BASE_URL = "http://127.0.0.1:8080"
$env:WEREWOLF_WS_URL = "ws://127.0.0.1:8080"
```

---

## 目录结构

```text
scripts/
├── README.md              # 本文档
├── requirements.txt       # websocket-client, requests
├── bootstrap.py           # 入口脚本 sys.path 辅助
├── lib/                   # Formal 路径 B 共享库（HTTP + WS Bot）
├── formal/                # 路径 B：/api/room + /ws/game
├── internal/              # 路径 A：/internal/game（开发/状态机）
├── dev/                   # 本地开发环境配置
├── reports/               # formal_llm_smoke 输出（gitignore）
└── action_logs/           # export_action_log 输出（gitignore）
```

---

## 脚本一览

### Formal 路径 B（`formal/`）

产品联调、Day4 五项、网关验收。**不能**用 Internal 脚本替代。

| 脚本 | 作用 |
|------|------|
| [`run_day4_formal.py`](formal/run_day4_formal.py) | **PRD §8.2 Day4 主验收**：12 Bot WS 同房、JOIN/PHASE_SYNC、狼座 ACK、phase-tick 终局；`--parallel-rooms` 双房隔离 |
| [`formal_path_smoke.py`](formal/formal_path_smoke.py) | 全链路 8 项快速冒烟（建房→WS→start→tick→GAME_OVER）；countdown 开启时可能 7/8 |
| [`countdown_observe.py`](formal/countdown_observe.py) | **P-05 首选**：订阅约 35s，断言同阶段 `countdown` 递减 |
| [`formal_llm_smoke.py`](formal/formal_llm_smoke.py) | Formal + DeepSeek LLM 整局；校验 `action_log` 含 modelId/thinking，报告见 `reports/` |

```bash
# 启动后端后，在仓库根目录：
python scripts/formal/run_day4_formal.py
python scripts/formal/run_day4_formal.py --parallel-rooms
python scripts/formal/countdown_observe.py
python scripts/formal/formal_path_smoke.py
python scripts/formal/formal_llm_smoke.py
```

### Internal 路径 A（`internal/`）

绕过网关，直接测状态机 / Mock AI。**报告须标注路径 A**，不能证明 Formal 协议闭环。

| 脚本 | 作用 |
|------|------|
| [`auto_play_client.py`](internal/auto_play_client.py) | 一键 `mock-auto-play` 跑完整局 |
| [`tick_play_client.py`](internal/tick_play_client.py) | 逐步 `phase-tick` 推进至 GAME_OVER |
| [`export_action_log.py`](internal/export_action_log.py) | 导出 `action_log` JSON；`--play` 先跑局，`--validate` 校验阶段/动作 |

```bash
python scripts/internal/auto_play_client.py
python scripts/internal/tick_play_client.py
python scripts/internal/export_action_log.py --play --validate
```

### 开发工具（`dev/`）

| 脚本 | 作用 |
|------|------|
| [`set-dev-env.ps1`](dev/set-dev-env.ps1) | Windows：持久化 `DEEPSEEK_API_KEY`、`SPRING_PROFILES_ACTIVE=dev` |

```powershell
.\scripts\dev\set-dev-env.ps1 -ApiKey "sk-..."
```

### 共享库（`lib/`）

供 `formal/run_day4_formal.py` 等复用，也可在自定义脚本中：

```python
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))  # scripts/
import bootstrap
bootstrap.bootstrap()

from lib.bot_player import BotPlayer
```

| 模块 | 作用 |
|------|------|
| `config.py` | 服务地址、Token（userId） |
| `http_api.py` | `/api/room` HTTP 封装 |
| `ws_client.py` | `/ws/game` WebSocket 客户端 |
| `bot_player.py` | 单座 Bot（HTTP + WS） |
| `auto_action.py` | 根据 PHASE_SYNC 自动出招（Mock 友好） |
| `message_handler.py` | 消息解析与线程安全打印 |

---

## 双路径说明

| 路径 | 入口 | 典型脚本 |
|------|------|----------|
| **A — Internal** | `POST /internal/game/...` | `internal/auto_play_client.py` |
| **B — Formal** | `POST /api/room` + `WS /ws/game` | `formal/run_day4_formal.py` |

详见 [docs/reference/bot-load-test.md](../docs/reference/bot-load-test.md)、[testing-strategy.md](../docs/reference/testing-strategy.md)。

---

## 相关文档

- [gateway-integration.md](../docs/reference/gateway-integration.md)
- [bot-load-test.md](../docs/reference/bot-load-test.md)
- [ADR-005 gateway formal path](../docs/adr/005-gateway-formal-path.md)

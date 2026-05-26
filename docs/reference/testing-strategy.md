# 测试与验收策略

| 属性 | 值 |
|------|-----|
| 版本 | v0.1 |
| 日期 | 2026-05-18 |
| 验收基线 | [PRD §8.2 Day4](../progress/requirements-mvp-v0.1.md)、§11.3 |

---

## 1. 测试金字塔

```text
        ┌─────────────┐
        │ Bot E2E     │  12 同房、Day4 五项（Formal WS）
        ├─────────────┤
        │ Gateway IT  │  HTTP+WS 脚本 / 未来 @SpringBootTest
        ├─────────────┤
        │ game/ai UT  │  SM、Memory、Mock 整局（现有）
        └─────────────┘
```

---

## 2. 分层说明

### 2.1 单元测试（`src/test/java`）

| 范围 | 工具 | 环境 |
|------|------|------|
| `GameStateMachine`、规则、DeathBus、遗言 | JUnit 5、AssertJ | 无 Docker；`application.properties` 排除 DB/Redis/LLM |
| `AIService`、Memory 投影 | 同上 + `AIServiceRetryTest`（解析重试/Mock fallback）+ 可选 LLM 集成测 | |
| `MockAIFullGameTest` | 内存整局至 `GAME_OVER` | A 路径 P0 |

**命令**：`./mvnw.cmd test`

### 2.2 网关集成（待补）

| 项 | 方法 |
|----|------|
| HTTP 建房 / join×12 / start | `Invoke-RestMethod` 或 Python `urllib` |
| WS CONNECTED / JOIN / PHASE_SYNC / ACTION_ACK | `websocket-client` |
| 参考脚本 | 验收时使用的 HTTP+WS 流程，可固化为 `scripts/ws-smoke.py` |

**通过标准**：见 [gateway-room-ws-checklist §5](../gateway-room-ws-checklist.md)。

### 2.3 Internal 路径 E2E（已可用）

| 脚本 | 路径 | 验证 |
|------|------|------|
| `auto_play_client.py` | `scripts/internal/` | `mock-auto-play` → `GAME_OVER` |
| `tick_play_client.py` | `scripts/internal/` | `phase-tick` 逐步推进 |
| `export_action_log.py` | `scripts/internal/` | `action_log` 完整性 |

**注意**：报告须标注 **路径 A（internal）**，不能替代 Formal WS 验收。

### 2.4 Bot Formal E2E（C）

见 [bot-load-test.md](bot-load-test.md)。Day4 五项以 **路径 B** 为准。

### 2.5 Countdown（P-05）

| 脚本 | 路径 | 说明 |
|------|------|------|
| `countdown_observe.py` | `scripts/formal/` | WS 订阅约 35s，断言同阶段 `countdown` 递减；**P-05 首选验收** |
| `formal_path_smoke.py` | `scripts/formal/` | 全链路；countdown 开启时末项可能 7/8，**不**代表正式环境故障，见 [ADR-005 §14.1](../adr/005-gateway-formal-path.md) |

---

## 3. PRD §8.2 Day4 映射

| # | 检查项 | 测试层 |
|---|--------|--------|
| 1 | WS + `CONNECTED` | Gateway IT |
| 2 | `JOIN_ROOM` → `PHASE_SYNC` + 正确 `playerId` | Gateway IT + Bot E2E |
| 3 | `start` → `NIGHT_WOLF` / `ROLE_ASSIGN` | HTTP IT |
| 4 | `GAME_ACTION` → `ACTION_ACK` | Gateway IT |
| 5 | 12 Bot 同房不串房 | Bot E2E |

---

## 4. 压测成功定义（PRD §11.3）

| ID | 定义 | 路径 |
|----|------|------|
| T3 | 跑完一局且无死锁（`STUCK` 超时退出） | internal 或 WS + tick |
| T4 | 100 局成功率 > 95% | Week2；报告含 `roomId`、失败 `requestId` |

指标建议：局时长、终局 phase、`action_log` 条数、非法 `ACTION_ACK` 比例。

---

## 5. CI 建议（后续）

| Job | 内容 |
|-----|------|
| `test` | `mvnw test`（无 Docker） |
| `ws-smoke` | 可选：启动无 DB profile + `scripts/formal/formal_path_smoke.py` |
| `bot-internal` | `scripts/internal/auto_play_client.py` 对 dev 环境 |

---

## 6. 当前缺口（2026-05-18）

- 无 `gateway`/`room` 的 `@SpringBootTest`。
- Bot README 已合并至 `scripts/README.md`；Day4 以 Formal 路径为准。
- LLM 集成测依赖环境变量，本地可能 1 失败。

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-05-18 | 初稿 |

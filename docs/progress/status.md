# 项目状态（实现进度）

| 属性 | 值 |
|------|-----|
| 更新 | 2026-05-18 |
| 需求基线 | [PRD v1.0.14](requirements-mvp-v0.1.md) |
| ADR | [adr/](../adr/)（含 [005 Gateway 推送](../adr/005-gateway-push-and-phase-timer.md)） |
| B/C 架构 | [gateway-room-modules](../reference/gateway-room-modules.md) · [gateway-integration](../reference/gateway-integration.md) |

细节规则见 PRD；包结构见 [code-modules](../reference/code-modules.md)。

---

## 结论（一句话）

**`game` 内存态与 Mock/LLM 整局、Seat Memory 已闭环；Formal WS 链路部分可用，主动推送与 Bot Day4 未验收通过。**

| 维度 | 状态 |
|------|------|
| 状态机（夜/昼/死/猎/愚/遗言/屠边） | 基本完成 |
| `action_log`、AI LLM 骨架 | 部分 |
| `gateway` / `room` | **部分完成**（HTTP+WS 请求-响应；无推送/Redis） |
| Seat Memory（M3） | **已实现**（[ADR-004](../adr/004-ai-seat-memory.md)） |
| Bot Formal 联调 | 脚手架；待对齐 [bot-load-test](../reference/bot-load-test.md) |

测试（2026-05-18）：`mvnw.cmd test` → 58 测，1 失败（`AIServiceLlmIntegrationTest`，需 API Key）。

---

## 已完成要点

- PRD、架构/选型/设计文档、ADR 001～005、reference 联调文档集
- `GameStateMachine` 全阶段 + DeathBus + 遗言 R24
- `AiTurnCoordinator`、`GameViews`、`ActionLogService`、Seat Memory
- `MockGameRunner` + `MockAIFullGameTest`（路径 A）
- LangChain4j + DeepSeek
- `gateway`：`/ws/game`、`RoomController`、`MessageRouter`（拉取式 `PHASE_SYNC`）
- Bot：`auto_play_client` / `tick_play_client`（路径 A）

---

## 待办（按优先级）

### 引擎（A）

| ID | 项 | 状态 |
|----|-----|------|
| G-03 | `GAME_EVENT` 广播 | 未发 |
| G-04 | `action_log` 持久化 | 仅内存（见 [persistence-rollout](../reference/persistence-rollout.md)） |
| G-06 | 狼夜超时随机刀 | 未做 |
| A-02 P1 | 全角色 LLM + JSON 95% | 待做 |

### 网关 / Bot（B / C）— 见 [ADR-005](../adr/005-gateway-push-and-phase-timer.md)

| ID | 项 | 状态 |
|----|-----|------|
| P-01 | WS + HTTP 建房/join/start | **部分** |
| P-02 | `PHASE_SYNC` **主动推送** | 未做 |
| P-03 | 阶段定时 `tick` 挂接 WS | 仅 internal |
| P-04 | 12 Bot Formal 联调 | 未通过 |
| P-05 | Redis / 鉴权 / MySQL 热路径 | 未做 |
| P-06 | `aiCount`、自动分座 | 未做 |

### PRD P0 验收

| 项 | 路径 A（internal） | 路径 B（formal WS） |
|----|-------------------|---------------------|
| Mock 无人干预整局 | **是** | 否 |
| Day4 五项 | — | **否**（见 [testing-strategy](../reference/testing-strategy.md)） |
| `PHASE_SYNC` 倒计时 | 否 | 否 |
| 非法操作不污染状态 | **是**（单测） | 部分（需集成测） |

---

## Post-MVP / 后续功能（不挡上线）

| ID | 项 | 说明 |
|----|-----|------|
| **A-03** | **狼队两阶段夜** | MVP 保持 PRD R10/R11/R17a 现状 |

---

## 建议顺序

1. B：ADR-005 推送 + 房间锁（P-02、P-03）
2. C：Bot 对齐 + Day4 脚本（[bot-load-test](../reference/bot-load-test.md)）
3. G-04 + Redis 会话（[persistence-rollout](../reference/persistence-rollout.md)）
4. A-02 P1 全角色 LLM

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-05-18 | 精简进度页 |
| v1.1 | 2026-05-18 | ADR 四篇独立 |
| v1.2 | 2026-05-18 | M3 完成；A-03 登记 |
| v1.3 | 2026-05-18 | gateway 标部分完成；链 ADR-005 与 reference 文档 |

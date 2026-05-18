# ADR-005: Gateway 出站推送与阶段定时

| 属性 | 值 |
|------|-----|
| 状态 | **已采纳（Accepted）** — 2026-05-18 |
| 日期 | 2026-05-18 |
| 决策者 | B（gateway/room 牵头）；A/C 评审 |
| 关联 | [PRD §4.6、§6.2](../progress/requirements-mvp-v0.1.md)、[architecture §5.3、§7](../architecture/architecture-design-spec.md)、[gateway-room-modules](../reference/gateway-room-modules.md)、[ADR-003](003-ai-integration.md) |

## 背景

`game` + `ai` 已在内存态跑通整局；`gateway` 已实现 WS 接入与 **请求-响应** 式 `PHASE_SYNC` / `ACTION_ACK`，但 **无主动推送**、**无阶段定时器挂接**、**无 Redis 会话**。B/C 联调时易出现「协议在 PRD、行为在代码」不一致。

本 ADR 冻结 **出站推送语义**、**定时推进挂接点**、**与 SM 的并发边界**；不改变 PRD 已冻结的 `type` 全集与 `GamePhase` 枚举。

---

## 决策

### 1. 推送真源与裁剪

- **真源**：`GameEngineService.buildPhaseSync(roomId, seatId)`（内部 `PhaseSyncBuilder` + `GameViews`）。
- **裁剪规则**：与 AI `GameView`、PRD §4.6.5 **同一套**可见性；Gateway **不得**自行拼私密字段。
- **信封**：每条推送使用 `TargetedPhaseSync`：`{ seatId, phaseSync }` 外包在 `type: PHASE_SYNC` 下（见 [gateway-room-modules](../reference/gateway-room-modules.md) §3.3）。

### 2. 必须推送的事件

| 事件 | 接收者 | 附带消息 |
|------|--------|----------|
| `JOIN_ROOM` 成功 | 该座 | `PHASE_SYNC` |
| `start` 成功 | 房内已绑定 WS 的各座 | 各座 `PHASE_SYNC` |
| `submitAction` 成功且局面变化 | 受影响座集合（至少操作者） | `ACTION_ACK` + 相关座 `PHASE_SYNC` |
| `GamePhaseScheduler.tick` 推进 | 同房已连接座 | `PHASE_SYNC` |
| 愚者翻牌、死亡公布等 | PRD 规定的公开/私密集合 | `GAME_EVENT` + 必要时 `PHASE_SYNC` |

**禁止**：将 `ActionResult.phaseSyncs` 无 seat 标注地广播给全房。

### 3. 阶段定时器（不 busy-wait）

- **组件**：`GamePhaseScheduler`（`game.orchestration`），方法 `tick(roomId)`。
- **调用方（MVP）**：Gateway 内 **每房间** `ScheduledExecutorService` / `@Scheduled`，在阶段超时或 dev 轮询时调用；等价于 internal `POST .../phase-tick`。
- **禁止**：`while (phase unchanged) { sleep }` 占满虚拟线程。

`tick` 语义（与 [gateway-integration](../reference/gateway-integration.md) §2 一致）：

| `GamePhase` | 行为 |
|-------------|------|
| `NIGHT_DEATH_ANNOUNCE` / `EXILE_DEATH_ANNOUNCE` | `advanceDayAnnounce` |
| `NIGHT_WOLF` / `NIGHT_SEER` / `NIGHT_WITCH` / `DAY_DISCUSS` / `DAY_VOTE` / `HUNTER_SHOOT` / `LAST_WORDS` | `AiTurnCoordinator.tickOneStep`（AI 座） |
| `GAME_OVER` | 返回终局 |
| 其他 | `NO_OP` |

每次 `tick` 或 `submitAction` 返回后，Gateway **负责**按 §2 推送（当前代码待实现）。

### 4. `countdown`

- **权威**：SM 或房间级 `PhaseTimer` 持有剩余秒数（实现待定，可先在 `GameRoomState` 扩展字段）。
- **下发**：随 `PHASE_SYNC.countdown` 推送；客户端 **不**本地推算倒计时。
- **刷新**：定时器每秒或阶段切换时推送更新（MVP 可粗粒度：仅阶段切换时带 countdown）。

### 5. 单房间写串行

- 同一 `roomId` 的 `submitAction`、`tick`、`start` 须 **串行化**（房间锁或单线程执行器）。
- WS 入站、HTTP `start`、定时器回调 **共用**同一串行域，避免狼夜并发写冲突。

### 6. `GAME_EVENT` 来源（P1）

- SM / `DeathBus` 订阅者在状态变更时发布 **领域事件**；Gateway 订阅并转为 WS `GAME_EVENT`。
- MVP 可先推 `PHASE_SYNC` 覆盖愚者/死亡可见性；`GAME_EVENT` 与 PRD 示例对齐为 P1。

### 7. Redis 与会话（P2，不挡 ADR）

- 连接表 MVP 可用内存 `ConnectionManager`；**目标**仍按 PRD §4.7.2 同步到 `werewolf:ws:conn:{roomId}:{playerId}` 以支持重连。
- 重连 30s 窗口：token → 原 `playerId`，替换同 seat 的 session。

---

## 后果

### 正面

- B/C 联调有单一推送规范；与 A 的 `PhaseSyncBuilder` 一致，避免串座。
- 定时推进与 internal `phase-tick` 行为一致，便于 Bot 双路径验收。

### 负面 / 风险

- 推送量：阶段切换最多 12 条 `PHASE_SYNC`/次；MVP 可接受。
- 需补 Gateway 单测或脚本验证推送集合。

---

## 实现清单（B）

| ID | 项 | 优先级 |
|----|-----|--------|
| P-01 | `WsPushService`：`pushPhaseSync(roomId, seatId)` | P0 |
| P-02 | `JOIN_ROOM` / `start` / `submitAction` / `tick` 后调用推送 | P0 |
| P-03 | 房间级锁包裹 mutating 路径 | P0 |
| P-04 | 阶段 `Scheduled` 调 `GameEngineService.tickPhase` | P1 |
| P-05 | `countdown` 字段 | P1 |
| P-06 | `GAME_EVENT` 桥接 | P1 |
| P-07 | Redis 会话映射 | P2 |

---

## 参考实现（当前缺口）

- `ConnectionManager` 已有 `findBySeat`，**无** `send` 封装。
- `MessageRouter` 仅返回 JSON Map，由 `GameWebSocketHandler` 回给 **请求方** session。
- `GameWebSocketHandler` 未在 `start` 后广播。

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-05-18 | 初稿采纳 |

# 代码模块速查

包依赖与 PRD §7.2 一致：`gateway` → `room` / `game`；`game` → `ai`；`ai` 不依赖 `gateway`。

Gateway/Room 详见 [gateway-room-modules](gateway-room-modules.md)。

---

## `com.werewolfengine.game`

| 功能 | 子包 | 类 | 说明 |
|------|------|-----|------|
| **门面** | `game.engine` | `GameEngineService` | HTTP/网关入口：建房、提交动作、tick、Mock 自动对局 |
| **状态机** | `game.engine` | `GameStateMachine` | 权威局况；`handleAction` / `advanceDayAnnounce` |
| **编排** | `game.orchestration` | `AiTurnCoordinator`, `TurnActorResolver` | AI 选座与 SM 桥接 |
| **编排** | `game.orchestration` | `GamePhaseScheduler` | 网关 `phase-tick`；未到期返回 `COUNTDOWN` |
| **编排** | `game.orchestration` | `PhaseTimeoutHandler` | 阶段超时兜底（P-05） |
| **编排** | `game.orchestration` | `MockGameRunner` | dev/压测跑到 `GAME_OVER` |
| **同步** | `game.sync` | `PhaseCountdown`, `PhaseSyncBuilder` | 权威倒计时 + 按座 `PHASE_SYNC` |
| **视图** | `game.view` | `GameViews`, `GameView`, `SeatVisibility`, `SeatPerceptionProjector` | 按座可见性 + episodic 投影（ADR-004） |
| **开局** | `game.setup` | `RoleAssigner` | 发牌 |
| **夜晚** | `game.night` | `NightActions`, `NightSkillPipeline`, `NightResolver`, `WolfVoteResolver` | 夜技能与结算 |
| **胜负** | `game.win` | `WinChecker`, `GameOutcome` | 屠边 |
| **放逐** | `game.exile` | `ExileResolver` | 白天投票 |
| **猎人** | `game.hunter` | `HunterShootFlow` | 开枪 |
| **遗言** | `game.lastwords` | `LastWordsFlow` | R24 |
| **死亡** | `game.death` | `DeathBus`, subscribers | 死亡总线 |
| **模型** | `game.model` | `GameRoomState`, `GamePhase`, … | DTO |
| **HTTP** | `game.api` | `InternalGameController` | 内部 REST（Week1） |
| **可观测** | `game.observability` | `ActionLogService` | `action_log`（Memory 投影源） |

```text
game.api → game.engine | game.orchestration
game.engine → setup | sync | win | night | death | hunter | exile | lastwords | model
game.orchestration → game.engine | ai.api
game.view → sync
ai.* → game.view | game.engine（只读）| game.model
```

内部设计见 [ADR-001](../adr/001-night-skill-pipeline.md)、[ADR-002](../adr/002-death-bus-and-hunter-flow.md)。

---

## `com.werewolfengine.ai`

| 功能 | 子包 | 类 | 说明 |
|------|------|-----|------|
| **门面** | `ai.api` | `AIService` | `decide` / `requestLlmIntentWithRetries`（解析最多重试 2 次） |
| **契约** | `ai.api` | `PlayerIntent` | → `GameActionCommand` |
| **Agent** | `ai.agent` | `AiAgent` | 单座 Persona |
| **记忆** | `ai.memory` | `MemoryPromptFormatter` | Prompt「本局记忆」段；投影见 `game.view` |
| **感知** | `ai.perceive` | `GameViewContext` | 委托 `GameViews.forSeat` |
| **提示词** | `ai.prompt` | `AiPromptBuilder`, `Persona` | 记忆 + 当前局面 + 合法 action |
| **解析** | `ai.parse` | `AiIntentParser`, `AiActionJson` | LLM → intent；`normalizeLlmPayload` 修 DeepSeek `reasoning_content` |
| **策略** | `ai.policy` | `MockAIPlayer` | Mock / fallback |
| **校验** | `ai.guard` | `AiLegalActions` | 合法 action |
| **工具** | `ai.tools` | `GameTools` | Phase B，主路径未接 |
| **配置** | `ai.config` | `AiProperties`, `AiConfiguration` | `werewolf.ai.*` |

```text
game (AiTurnCoordinator) → ai.api.AIService
ActionLogService → game.view.SeatPerceptionProjector → ai.memory.MemoryPromptFormatter
ai.api → agent | memory | perceive | prompt | parse | guard | policy | config
ai 不依赖 gateway
```

### 数据流（M3）

```text
action_log → SeatPerceptionProjector → MemoryPromptFormatter → 「本局记忆」
GameViews → GameViewContext → 「当前局面」
→ AiPromptBuilder → ChatModel → AiIntentParser → PlayerIntent
```

编排见 [ADR-003](../adr/003-ai-integration.md)、[ADR-004](../adr/004-ai-seat-memory.md)。

### Game ↔ AI 调用链

```text
Gateway tick → GamePhaseScheduler → AiTurnCoordinator → AIService.decide
  → GameEngineService.submitAction → GameStateMachine
```

---

## `com.werewolfengine.gateway`

| 功能 | 类 | 说明 |
|------|-----|------|
| WS 入口 | `GameWebSocketHandler` | `CONNECTED`、`JOIN_ROOM`、`READY`；其余交 Router |
| 路由 | `MessageRouter` | `GAME_ACTION`、`PHASE_SYNC` → `GameEngineService` |
| 连接表 | `ConnectionManager` | `(roomId, seatId)` ↔ `WebSocketSession`（内存） |
| 配置 | `WebSocketConfig` | 注册 `/ws/game` |

```text
GameWebSocketHandler → ConnectionManager | RoomService | MessageRouter
MessageRouter → GameEngineService
```

Gateway 见 [ADR-005](../adr/005-gateway-formal-path.md)。

---

## `com.werewolfengine.room`

| 功能 | 类 | 说明 |
|------|-----|------|
| HTTP | `RoomController` | `/api/room` CRUD |
| 服务 | `RoomService` | 建房、join、ready；`start` → `GameEngineService` |

```text
RoomController → RoomService → GameEngineService
```

---

## `com.werewolfengine.message`

| 功能 | 类 | 说明 |
|------|-----|------|
| 类型 | `MessageType` | WS `type` 枚举，对齐 PRD |
| 载荷 | `PhaseSyncPayload` | `PHASE_SYNC` 字段 |
| 载荷 | `ActionAckPayload` | `ACTION_ACK` 字段 |

序列化：Gateway 当前用 `Map` + Jackson；后续可统一为 `message` DTO。

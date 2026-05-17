# werewolf-engine 项目进度报告

| 属性 | 值 |
|------|-----|
| 版本 | v0.1 |
| 更新日期 | 2026-05-17 |
| 需求基线 | [requirements-mvp-v0.1.md](requirements-mvp-v0.1.md) **v1.0.12**（含 R24 遗言） |
| 架构对照 | [architecture-design-spec.md](architecture-design-spec.md) v1.4 |

---

## 1. 总体结论

**`game` 模块：状态机主路径与 12 人板核心机制已在内存态实现并可测，但尚未达到 PRD P0「Mock AI 无人干预可跑通一整局 + 网关联调」的完整验收。**

| 维度 | 状态 |
|------|------|
| 游戏状态机主干（夜/昼/死/猎/愚/遗言/屠边） | **基本完成** |
| 规则边角、超时调度、事件与日志 | **未完成或仅部分** |
| `gateway` / `room` / `bot` / 持久化 | **未开始** |
| 全自动 Mock 整局 | **未闭环** |

本地验证（2026-05-17）：`mvnw.cmd test` → **18** 个测试，**0** 失败（含 `GameStateMachineTest` 13 + 剧本演示 4 + `GameFlowDemoTest` 1 + Spring 上下文 1）。

> 说明：R24 遗言、多夜剧本测试、`ExileResolver` 白痴翻牌后进下一夜等改动可能仍在工作区，**以 `git status` 为准**。

---

## 2. 已完成（`game` + 文档）

### 2.1 文档与规范

- MVP PRD v1.0.12（R1～R24、§4.3.8 引擎窄版）
- 架构设计说明书 v1.4、技术选型、ADR-001/002、本地环境说明
- Cursor 项目上下文规则（`.cursor/rules/werewolf-engine-context.mdc`）

### 2.2 工程骨架

- Java 21 + Spring Boot 4.0.6
- 依赖：Web、WebSocket、JPA、Redis、LangChain4j（**业务未接**）
- `docker-compose`：MySQL 8 + Redis 7
- 包结构：`game`、`ai`、`message`（**无** `gateway`、`room`、`bot`）

### 2.3 状态机与 `GamePhase`

已实现并可测试的阶段流转（内存 `GameStateMachine`）：

```text
WAITING → ROLE_ASSIGN → NIGHT_START → NIGHT_WOLF → NIGHT_SEER → NIGHT_WITCH
  → [夜末结算 / R23] → NIGHT_DEATH_ANNOUNCE → [LAST_WORDS 首夜有条件]
  → [HUNTER_SHOOT] → DAY_DISCUSS → DAY_VOTE → VOTE_RESULT
  → [EXILE_DEATH_ANNOUNCE] → [LAST_WORDS 放逐] → [HUNTER_SHOOT] → CHECK_WIN
  → 下一夜 / GAME_OVER
```

### 2.4 核心机制（对照 PRD）

| 能力 | 实现落点 | 备注 |
|------|----------|------|
| 12 人发牌（4 狼 4 民 1 愚 1 预 1 女 1 猎） | `RoleAssigner` | 随机洗牌 |
| 狼刀跟票 R10、刀狼商议 R17a | `NightActions`、`WolfVoteResolver` | |
| 预言家查验 R12 | `NightActions` | GOOD/WOLF |
| 女巫救/毒/跳过 R3/R4/R5 | `NightActions`、`NightResolver` | 同夜不可救+毒 |
| 夜末死亡结算 | `NightResolver` + `DeathBus` | |
| 天亮/放逐死讯公布 | `NIGHT_DEATH_ANNOUNCE`、`EXILE_DEATH_ANNOUNCE`、`advanceDayAnnounce` | |
| 遗言 R24 | `LastWordsFlow`、`GamePhase.LAST_WORDS` | 首夜死者 / 放逐者 |
| 猎人开枪 R7/R8/R9 | `HunterShootFlow`、`HunterPendingSubscriber` | 毒杀不开枪 |
| 白天讨论 R13 | `enterDayDiscuss`、发言顺序 | 锚点 + 顺/逆时针 |
| 投票、平票 R14 | `resolveDayVote` | 最高票唯一才放逐 |
| 愚者翻牌 R19～R22 | `ExileResolver` | 翻牌后进入下一夜 |
| 屠边胜负 R1/R21、即时判胜 R23 | `WinChecker`、`GameOutcome.tryEndGame` | 含最后一神猎人被票 |
| 定向 `PHASE_SYNC` 骨架 | `PhaseSyncBuilder` | 子集字段 |
| 临时 HTTP 联调 | `InternalGameController`、`GameEngineService` | 无鉴权，非对外协议 |
| Mock AI 骨架 | `MockAIPlayer` | **仅狼夜随机 KILL** |

### 2.5 测试与剧本

| 测试 | 说明 |
|------|------|
| `GameStateMachineTest` | 狼夜、死讯、猎人、胜负、遗言、平票等 |
| `GameFlowDemoTest` | 第一夜控制台演示 |
| `GameScenarioDemoTest` | 三夜狼胜、五夜好人胜、四夜狼胜（最后一神被票）、五夜好人胜（自刀/平票） |

运行示例：

```powershell
mvnw.cmd test
mvnw.cmd test -Dtest=GameScenarioDemoTest
```

---

## 3. 未完成 / 待办（按优先级）

### 3.1 规则与引擎边角（A / `game`）

| ID | 项 | PRD/说明 | 状态 |
|----|-----|---------|------|
| G-01 | **R2** 最后一狼与最后一民同夜同死 → 狼赢 | §3.2 R2 | 未实现 / 无单测 |
| G-02 | **阶段超时与兜底** | §4.3.3 各阶段 countdown、超时默认行为（狼随机刀、女巫 SKIP、预空过等） | 无调度器；依赖测试手动 `advanceDayAnnounce` / `handleAction` |
| G-03 | **`GAME_EVENT` 广播** | 如 `IDIOT_REVEALED` | `MessageType` 有定义，局内未发送 |
| G-04 | **`action_log` 结构化日志** | §0.4、§4.7 P0 可观测 | 未接入热路径 |
| G-05 | **`SPEAK` / `WOLF_CHAT` 正文 `content`** | §4.5、`GameActionCommand` | 当前 command **无 content 字段**，发言不落库 |
| G-06 | **狼夜无票型时随机刀非狼** | §4.3.3 `NIGHT_WOLF` 超时兜底 | SM 内未自动执行 |
| G-07 | **`DayResolver` 独立模块** | §4.4.3、§7.1 | 逻辑在 `GameStateMachine` + `ExileResolver`，未拆类（能力大致等价） |
| G-08 | **`ROLE_ASSIGN` / `NIGHT_START` 节奏** | 对外推送与定时 | 开局快速掠过，无独立超时 |
| G-09 | **`CHECK_WIN` 对外语义** | 协议图 | 内部经过，多数情况 R23 直接 `GAME_OVER` |
| G-10 | **座位与账号** | `playerId` 1～12 已用；`userId`↔座位 | 仅 `PlayerState`，**无** `room` 绑定 |

### 3.2 网关、房间、Bot（B / C — PRD P0 联调）

| ID | 项 | 负责人 | 状态 |
|----|-----|--------|------|
| P-01 | **`gateway`** 原生 WebSocket、`GAME_ACTION` / `ACTION_ACK` / `PHASE_SYNC` 推送 | B | 未开始 |
| P-02 | **`room`** HTTP 建房、ready、12 人、`userId` 映射 | B | 未开始 |
| P-03 | **Redis** 连接映射 `playerId ↔ sessionId` | B | 未开始 |
| P-04 | **`bot/`** 12 路并发、同房不串房 | C | 未开始 |
| P-05 | **MySQL** 对局与玩家持久化、`game_record` | B | JPA 依赖在，**热路径未接** |

### 3.3 AI 与自动化（A — Week1/2）

| ID | 项 | 状态 |
|----|-----|------|
| A-01 | **12 座 Mock AI 自动打满一整局**（无人干预） | 未闭环 |
| A-02 | **LangChain4j + DeepSeek** 全角色 AI | Week2，未接 |
| A-03 | **`GameView` / `legalActions`（§4.3.8 M4）** | P1，未做 |

### 3.4 PRD P0 验收项对照

| 验收项（§1.2 / §8） | 当前 |
|---------------------|------|
| 从发牌到胜负，**Mock 无人干预**可跑通一整局 | **否**（需 Bot + 阶段推进 + 全角色 Mock） |
| 存活玩家均能收到 `PHASE_SYNC`，倒计时误差 ≤ 1s | **否**（无 WS、无定时） |
| 非法操作返回 `ERROR`，不污染状态 | **是**（单测覆盖部分路径） |
| 场景 S2 纯 AI / 压测多局 | **否** |

---

## 4. 建议实施顺序

1. **G-02 + P-01**：阶段超时契约 + 网关定时调用 `advanceDayAnnounce` / 推进投票（可与 B 对齐 §4.3.3 表）。
2. **G-03 + G-04**：`GAME_EVENT`（愚者翻牌等）+ `action_log`，满足可观测 P0。
3. **G-01 + G-05 + G-06**：R2 单测、发言 `content`、狼夜超时 Fallback。
4. **P-02～P-05 + A-01**：房间 + WS + Bot，跑通 Week1 Day4～Day6「完整 Mock 一局」。
5. **A-02**：Week2 LLM 接入。

---

## 5. 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-05-17 | 初版：汇总 `game` 已完成项与 PRD P0 未完成项（含 R24 遗言与剧本测试现状） |

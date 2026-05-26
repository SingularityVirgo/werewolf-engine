# 项目状态（实现进度）

| 属性 | 值 |
|------|-----|
| 更新 | 2026-05-27 |
| 需求基线 | [PRD v1.0.17](requirements-mvp-v0.1.md) |
| ADR 索引 | [adr/README.md](../adr/README.md) |
| Gateway | [ADR-005](../adr/005-gateway-formal-path.md) |

细节规则见 PRD；包结构见 [code-modules](../reference/code-modules.md)；持久化路线见 [persistence-rollout](../reference/persistence-rollout.md)。

---

## 结论（一句话）

**规则引擎 + Formal 协议 P0 已通**（Mock/LLM 整局、Seat Memory、Day4 10/10、P-05 countdown、**G-06 狼夜超时兜底**）；距 PRD-MVP 完整产品形态仍差 **全角色 LLM 正式验收 + 压测**、**房间产品化（断线重连/持久化/鉴权）**；**Web UI（`frontend/`）可进局但展示层未接全 WS 推送**。

| 维度 | 状态 |
|------|------|
| 状态机（夜/昼/死/猎/愚/遗言/屠边） | 基本完成 |
| `action_log`、AI LLM | **LLM 优先 + Mock 兜底**；A-02 验收报告见 `target/reports/a02-full-game-*.json` |
| `gateway` / `room` | **P0 完成**（[ADR-005](../adr/005-gateway-formal-path.md) §13）；P1 Redis/断线等待做 |
| Seat Memory（M3） | **已实现**（[ADR-004](../adr/004-ai-seat-memory.md)） |
| Bot Formal 联调 | **Day4 10/10**（`scripts/formal/run_day4_formal.py`，2026-05-25） |
| **Web UI**（`frontend/`） | **MVP 可玩**：三屏 + WS 展示层（F-01～F-05）✅；压测/E2E 脚本见 `scripts/formal/frontend_ws_smoke.py` |

**测试（2026-05-27）**

| 项 | 结果 |
|----|------|
| `mvnw.cmd test` | **85** 测全过（含 `AIServiceRetryTest`、LLM 集成需 `DEEPSEEK_API_KEY`） |
| `scripts/formal/formal_path_smoke.py` | 联调 7/8 常见（countdown 下末项）；曾 8/8（无 P-05） |
| `scripts/formal/countdown_observe.py` | P-05：WS `countdown` 递减 |
| `scripts/formal/formal_llm_smoke.py` | 整局 `GAME_OVER`（124 tick）；`action_log` 49 条 LLM（`deepseek-v4-flash`） |
| `scripts/formal/run_day4_formal.py` | **10/10** Day4 五项 |

---

## PRD MVP 已闭环

| PRD 条目 | 状态 |
|----------|------|
| §1.2 P0：12 人 Mock 无人干预整局 | ✅ 路径 A（internal）+ 路径 B（Formal phase-tick） |
| §3 规则表 R1～R24、屠边、遗言、猎人 | ✅ `GameStateMachine` |
| §4.3 状态机全 `GamePhase` | ✅ |
| §4.5 Mock AI + LLM 骨架（LangChain4j / DeepSeek） | ✅ P0.5 狼夜可演示；全角色 LLM 已能跑局 |
| §4.6 WS 基础：`CONNECTED` / `JOIN_ROOM` / `GAME_ACTION` / `PHASE_SYNC` / `ACTION_ACK` | ✅ |
| §8.2 Day4 五项 | ✅ `run_day4_formal.py` |
| §8.5 Formal Mock/AI 整局 | ✅ |
| ADR-004 Seat Memory | ✅ |
| ADR-005 P-01～P-05（推送、锁、tick、定时、countdown） | ✅ |

---

## PRD MVP 剩余任务（对照基线）

### §1.2 业务目标

| 优先级 | PRD 目标 | 缺口 | 负责 |
|--------|----------|------|------|
| **P0** | 实时阶段同步，倒计时误差 ≤ 1s | ✅ P-05：权威 deadline + WS 递减（`countdown_observe.py`） | B |
| **P0** | 非法操作不污染状态 | 单测 ✅；Formal 缺系统化 WS 非法用例集 | A/B |
| **P1** | 真人 + AI 混合（S1） | 建房 `aiCount` / `join` 自动分座 ✅；对局掉线托管、混合调度未做 | B |
| **P1** | AI JSON 50 次解析率 > 95%，**6s** 超时、解析**最多重试 2 次**后 fallback | ✅ A-02；PRD v1.0.17 已升；**代码待** `llmTimeoutSeconds=6` + 重试循环 | A |
| **P1** | 离开 / 30s 重连 / 掉线托管 | §2.3、§4.2 未做 | B |

### §4.2 房间（B）

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| R-02 | `join` 自动分座（不传 `seatId`） | P0 | ✅ |
| — | 建房 `aiCount` 预占位 | P0 / S1 | ✅（`start` 前 AI 座自动 ready） |
| R-05 | `DELETE /room/{id}` 解散 | P0 接口已冻结 | ✅ |
| R-06 | 房间快照座位详情完整 | P0 | ✅（`GET` 含 `seats[]`） |
| — | 离开房间 / 掉线标记 | P1 | **部分**（`WAITING` 离开 ✅；对局中掉线未做） |

### §4.3 状态机 / 法官（A）

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| — | 阶段超时兜底（狼夜 30s 随机刀、投票弃票等） | §4.3.3 | ✅ `PhaseTimeoutHandler` + `NightActions.forceResolveWolfPhaseOnTimeout` / `applyTimedDayVoteFallback` |
| — | `NIGHT_WITCH` / `NIGHT_SEER` 阶段跑满 countdown | §4.3.7 | **是**（P-05；无角色时阶段末 fallback） |
| G-03 | SM 发出 `GAME_EVENT` | §4.6 | ✅ `PerceptionLogEvents` → outbound 队列 → WS |
| G-06 | 狼夜超时随机刀非狼 | §4.3.3 | ✅ |

### §4.5 AI（A）— Week2

| 里程碑 | PRD | 状态 |
|--------|-----|------|
| P0.5 狼夜 LLM + `action_log` | §4.5 | ✅ |
| P1 / **A-02** 全角色 LLM + 解析 ≥95% | §8.3 | ✅ 2026-05-26（内存整局 + `target/reports/a02-full-game-*.json`） |
| — | LLM 6s 超时 + JSON 解析重试 | §4.5.4 / §5 | ✅ `llm-timeout-seconds=6`、`max-llm-retries=2` + `AIServiceRetryTest` |
| — | `requestId` 贯穿 WS ↔ AI 日志 | §5 | 弱/未系统做 |
| — | `GameTools` @Tool | ADR-003 | Week2 不接入主路径 |

### §4.6 消息（B）

| ID | 项 | 状态 |
|----|-----|------|
| P-05 | `PHASE_SYNC.countdown` 权威下发 | ✅ |
| P-06 | `GAME_EVENT` WS 桥接 | ✅ |
| — | `CHAT_MESSAGE` + `scope=WEREWOLF` | ✅（兼 `GAME_ACTION/WOLF_CHAT`） |
| P-08 | 推送收窄 `affectedSeats` | ❌ MVP 推全连接座 |
| — | 断线重连 30s | ❌ |
| — | 连接心跳 | ❌ |
| — | 独立 `GAME_OVER` WS 消息 | ✅ |

### §4.7 持久化（B）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 0（当前） | 内存 `action_log` + 内存房间 | ✅ |
| 1 | MySQL `room` / `room_player` | ❌ |
| 2 | `game_record` + action_log 归档 | ❌ **G-04** |
| 3 | Redis 会话 / phase 镜像 / wolf 门闩 | ❌ **P-07** |
| P1 | JWT / 用户表 | ❌ |

### §5 非功能

| 指标 | 目标 | 现状 |
|------|------|------|
| 阶段切换（不含 LLM） | < 500ms | 系统阶段即时；未系统打点 |
| AI P95 | < **6s**（PRD v1.0.17） | 配置已对齐 6s；LLM 冒烟 avg 2.25s，max 4.7s（旧 3s 口径） |
| 100 局无状态异常 | 压测 | ❌ 报告 v0.1 未出 |
| 信息隔离 | 私密字段不串座 | sync 裁剪 ✅；M3 狼聊隔离 ✅；定向推送压测不足 |

### §8 里程碑

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| §8.1 D5-6 | 压测 20 局 | ❌ |
| §8.3 D4-5 | 全角色 AI + **100 局压测报告** | ❌（C 主责） |
| §8.3 D6-7 | 真人 + Bot 混合局 | 部分（建房/分座 ✅；断线/托管未做） |
| §8.4 Day7 | 协议/Schema 冻结会 | 文档 ✅；**前端 payload 与后端 WS 仍有 gap**（§前端） |

---

## 前端（C）— `frontend/`

> **UI 规格真源：** [frontend-ui-spec.md](../reference/frontend-ui-spec.md)（三屏、6+6+center、烛火法庭视觉）。  
> **2026-05-26 联调结论**：后端 Formal 路径可单人建房 → 准备 → 开局 → `phase-tick` 自动推进。

### 已通（后端 + Vite 代理）

| 项 | 说明 |
|----|------|
| 目录 | React + Vite，`frontend/` |
| HTTP | `POST /api/room` 建房、`/join` / `/ready` / `/start`；`boardType` 预留 |
| WS | `CONNECTED` → `JOIN_ROOM` → `PHASE_SYNC` / `ACTION_ACK` |
| 阶段 | 顶栏阶段名、倒计时（P-05）、操作面板（`canAct` 时） |
| 启动 | 后端 `:8080` + `cd frontend && npm run dev` → `:3000` |

> **布局说明：** 旧 4×3 网格 + 右侧边栏 **已过时**；目标布局见 `frontend-ui-spec.md` §4。

### 已接（2026-05-26 MVP 打通）

| ID | 状态 | 落点 |
|----|------|------|
| **F-01** | ✅ | `useWebSocket` → `CHAT_BROADCAST` 映射 `seatId` / `isWolfChat` |
| **F-02** | ✅ | `CenterStage` 公频/狼频道 Tab；`NIGHT_WOLF` 狼夜中间区 |
| **F-03～F-04** | ✅ | `GAME_EVENT` → `gameLog` + `gameEventFormat.ts` 中文宣读 |
| **F-05** | ✅ | `GAME_OVER` WS + `GameOverScreen`；`PHASE_SYNC` 终局保留已有 `winner`/`finalRoles` |
| **F-07～F-12** | ✅ | `App` 三屏、`GameTableSideLayout`、`SetupPage`+`boardType`、`HomePage` brand |
| **F-06** | 可选 | Mock AI `SKIP_SPEAK` 比例高，非协议问题 |

### 协议对照（冻结给 C，后端勿改除非 PRD 变更）

**`CHAT_BROADCAST` payload（后端 → 前端）**

```json
{ "scope": "WEREWOLF|ALL", "playerId": 3, "content": "…", "phase": "NIGHT_WOLF", "round": 1 }
```

**`GAME_EVENT` payload**

```json
{ "eventType": "NIGHT_DEATHS|EXILE_ANNOUNCED|HUNTER_SHOT|IDIOT_REVEALED|…", "data": { … } }
```

参考实现：`WsPushService.buildPayload`、`OutboundMessage.chatBroadcast` / `enqueueNightDeaths`。

### C 待办索引

| ID | 项 | 优先级 | 状态 |
|----|-----|--------|------|
| **F-13** | 狼夜 `KILL` / `ACTION_ACK` 反馈（PRD §4.8 **S-UI-04**） | **P0** | **未做** |
| — | 多人同房 UI | P1 | 未做 |
| — | 加入他人房间 UI | P1 | 未做 |
| — | E2E 冒烟 | P2 | `scripts/formal/frontend_ws_smoke.py` |

**F-13 范围（2026-05-26 联调结论）**

- **现象**：人类狼人选目标点「击杀」后界面几乎无变化，易被误判为按钮无效。
- **后端**：`KILL` 通常已成功（`wolfKillVotes` 记票）；阶段仍 `NIGHT_WOLF` 直至齐票或 30s 超时（R10），属预期。
- **前端缺口**：成功无 Toast；发送后立刻清空选中；`AnnouncerFeed` 不展示 `gameLog.type==='action'`；未展示 `playerSubState=WAITING_WOLF_CONSENSUS`。
- **验收**：按 PRD S-UI-04 四条；失败 `WOLF_CHAT_REQUIRED` 须 Toast + 事件区可见。

---

## 已完成要点

- PRD v1.0.17、架构文档、ADR 001～006、reference 联调集
- `GameStateMachine` 全阶段 + DeathBus + 遗言 R24
- `AiTurnCoordinator`、`GameViews`、`ActionLogService`、Seat Memory
- 路径 A：`MockGameRunner`、`MockAIFullGameTest`、`InternalGameController`
- LangChain4j + DeepSeek（`AIServiceLlmIntegrationTest`）
- **Gateway P0**：`WsPushService`、`RoomExecutionGuard`、`RoomPhaseTickScheduler`、`ConnectionManager` Bean；`POST /api/room/.../phase-tick`；WS `PHASE_TICK`（dev）
- **P-05**：`PhaseCountdown` + `PhaseTimeoutHandler`；`PHASE_SYNC.countdown` 递减推送（见 [gateway-integration §6](../reference/gateway-integration.md)）
- **room**：建房 / `aiCount` 预占位 / `join` 自动分座 / ready / start / snapshot / 解散
- **Bot**：路径 A `auto_play` / `tick_play`；路径 B `run_day4_formal` + `http_api` / `ws_client`

---

## 待办索引（按负责人）

### 引擎（A）

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| A-02 | 全角色 LLM + JSON 解析 ≥95%（50 次） | §1.2 P1、§8.3 | ✅ |
| G-03 | `GAME_EVENT` 从 SM 发出 | §4.6 | ✅ |
| G-04 | `action_log` MySQL 持久化 | §4.7 | 仅内存 |
| G-06 | 狼夜超时随机刀（SM 兜底） | §4.3.3 | ✅ |
| — | 阶段超时兜底（与 P-05 联动） | §4.3.3、§4.3.7 | ✅ |
| — | LLM 6s + JSON 解析重试（`max-llm-retries=2`） | §4.5.4 / §5 | ✅ `AIServiceRetryTest` |
| **G-08** | `action_log` 玩家动作重复写入（Coordinator + Engine 双记） | §4.7.3 | **未做**（后续） |
| **UX-01** | 发牌体验 / 身份统计（见下） | §4.3.3 | **未做**（后续） |

**UX-01 背景（2026-05-27，暂不实现）**

- **反馈**：真人玩家主观感觉「大概率是狼人」。
- **结论（代码审计）**：[`RoleAssigner`](../src/main/java/com/werewolfengine/game/setup/RoleAssigner.java) 对 12 身份 `Collections.shuffle` 后按座位发牌，**不区分** `humanUserId`；单局狼人概率 **4/12**。房主固定 **1 号位** 不提高狼率。
- **体验因素**：狼人夜有操作面板（刀/狼聊），村民夜多「等待行动」+ phase-tick 推进，易形成「有操作 ≈ 当狼」的记忆偏差。
- **后续可选**（产品评审后再做）：连局同用户不重复同一身份；对局结束身份统计/导出；帮助文案说明基础概率。MVP 仍保持纯随机发牌。

### 网关 / 房间（B）— [ADR-005 §13](../adr/005-gateway-formal-path.md)

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| P-01～P-05 | 推送、锁、tick、定时、countdown | §8.5 | **完成** |
| P-06 | `GAME_EVENT` WS 桥接 | §4.6 | ✅ |
| P-07 | Redis 会话 / token 鉴权 | §4.7.2、§2.3 | 未做 |
| P-08 | 推送收窄 `affectedSeats` | ADR-005 | 未做 |
| R-02 | `join` 自动分座 | §4.2 | **完成** |
| — | `aiCount` 建房 | §4.2、S1 | **完成** |
| R-05 | 房间解散 | §6.1 | **完成** |
| — | 断线重连 30s、心跳 | §2.3、checklist G-07 | 未做 |
| — | `CHAT_MESSAGE` 狼频道 | §4.6 | ✅ |

### Bot / 联调（C）

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| F-01～F-05 | Web UI WS 展示层（聊天/事件/宣读/结算） | §4.6 UI | **是**（2026-05-26） |
| F-13 | 狼夜击杀 / `ACTION_ACK` 反馈 | §4.8 S-UI-04 | **否**（见 §前端 C 待办） |
| — | 压测报告 v0.1（**100 局**，Formal 路径） | §8.3 | 待做 |
| — | 压测 20 局（Week1 D5-6） | §8.1 | 待做 |
| — | `--full-bot` 12 座纯 Bot 实验路径 | — | 可选 |
| — | Formal WS 非法操作用例集 | §1.2 P0 | 待做 |

---

## PRD 验收对照（§8）

| 项 | 路径 A（internal） | 路径 B（formal） |
|----|-------------------|------------------|
| Mock 无人干预整局 | **是** | **是**（phase-tick + Mock/AI） |
| Day4 五项（§8.2） | — | **是**（2026-05-25） |
| `PHASE_SYNC` 主动推送 | N/A | **是**（按座裁剪；MVP 推全连接座） |
| `PHASE_SYNC.countdown` | 否 | **是**（权威递减；`countdown_observe.py`） |
| 非法操作不污染状态 | **是**（单测） | **部分**（Day4 基础断言） |
| 12 Bot 同房不串房 | N/A | **是**（Day4） |
| 全角色 LLM 95% 解析 | 单测 + 整局验收 | **是**（A-02 报告） |
| 100 局压测无异常 | — | **否** |
| Web UI 聊天/死亡宣读 | — | **是**（F-01～F-05） |

> PRD §8.2 清单项仍为文档勾选格式；**实现结论以本页与 PRD §8.5 为准**。

---

## Post-MVP

| ID | 项 | 说明 |
|----|-----|------|
| A-03 | 狼队两阶段夜 | 不挡 MVP |
| — | JWT / 用户体系、复盘 API | PRD §1.2 P2 |
| — | 观战 UI 后端席位 | PRD §1.3 不做 |
| — | 多实例 / 集群 | PRD §1.3 不做 |

---

## 建议下一步

1. **P-08 / 断线重连**：推送收窄、30s 重连
3. **C**：Formal 路径 100 局压测报告 v0.1；**前端 F-01～F-04**（A/B 不阻塞）
4. **G-04 + P-07**：[persistence-rollout](../reference/persistence-rollout.md) 阶段 1～3

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-05-18 | 精简进度页 |
| v1.4 | 2026-05-25 | B P0；68 测；Bot Day4 |
| v1.5 | 2026-05-25 | 文档重组；PRD v1.0.15 §8.5；Gateway ADR 统一为 005-gateway-formal-path |
| v1.6 | 2026-05-25 | 增补 PRD MVP 全量对照（§1.2～§8 缺口表、非功能、里程碑、负责人索引） |
| v1.7 | 2026-05-25 | 拉齐一句话结论、§1.2 S1、ADR-005 P-05、单测 83/1 失败 |
| v1.8 | 2026-05-26 | M3 狼聊隔离：`AIService` 保留 WOLF_CHAT 门禁；83 测全过 |
| v1.9 | 2026-05-26 | §前端：`frontend/` 联调缺口 F-01～F-06（聊天/事件/宣读；后端已推送） |
| v2.0 | 2026-05-26 | A P0：G-06 狼夜超时随机刀；§4.3.3 阶段兜底（投票弃票、分阶段 handler） |
| v2.1 | 2026-05-26 | A-02：LLM 优先 / Mock 兜底；`A02FullGameLlmAcceptanceTest` + `target/reports/a02-full-game-*.json` |
| v2.2 | 2026-05-26 | Web UI MVP：F-01～F-12、`hostUserId` 建房/开局、`frontend_ws_smoke.py` |
| v2.3 | 2026-05-26 | PRD v1.0.16 **S-UI-04**；待办 **F-13**（狼夜 KILL / ACTION_ACK 反馈，联调 backlog） |
| v2.4 | 2026-05-27 | 待办 **UX-01**：真人发牌体验（审计结论 + 后续可选，暂不实现） |
| v2.5 | 2026-05-27 | 全库文档对齐 PRD **v1.0.17**（6s/重试/ADR-006）；待办 **G-08** action_log 去重 |

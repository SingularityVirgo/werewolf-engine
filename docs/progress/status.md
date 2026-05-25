# 项目状态（实现进度）

| 属性 | 值 |
|------|-----|
| 更新 | 2026-05-25 |
| 需求基线 | [PRD v1.0.15](requirements-mvp-v0.1.md) |
| ADR 索引 | [adr/README.md](../adr/README.md) |
| Gateway | [ADR-005](../adr/005-gateway-formal-path.md) |

细节规则见 PRD；包结构见 [code-modules](../reference/code-modules.md)；持久化路线见 [persistence-rollout](../reference/persistence-rollout.md)。

---

## 结论（一句话）

**规则引擎 + Formal 协议 P0 已通**（Mock/LLM 整局、Seat Memory、Day4 10/10）；距 PRD-MVP 完整产品形态仍差 **阶段倒计时/超时法官**、**全角色 LLM 正式验收 + 压测**、**房间产品化（aiCount/重连/持久化/鉴权）**。

| 维度 | 状态 |
|------|------|
| 状态机（夜/昼/死/猎/愚/遗言/屠边） | 基本完成 |
| `action_log`、AI LLM | 狼夜/整局可跑；**A-02** 全角色 JSON ≥95% 待验收 |
| `gateway` / `room` | **P0 完成**（[ADR-005](../adr/005-gateway-formal-path.md) §13）；P1 Redis/断线等待做 |
| Seat Memory（M3） | **已实现**（[ADR-004](../adr/004-ai-seat-memory.md)） |
| Bot Formal 联调 | **Day4 10/10**（`bot/run_day4_formal.py`，2026-05-25） |

**测试（2026-05-25）**

| 项 | 结果 |
|----|------|
| `mvnw.cmd test` | **68** 测全过（含 LLM 集成，需 `DEEPSEEK_API_KEY`） |
| `scripts/formal-path-smoke.py` | 联调 7/8 常见（countdown 下末项）；曾 8/8（无 P-05） |
| `scripts/countdown-observe.py` | P-05：WS `countdown` 递减 |
| `scripts/formal-llm-smoke.py` | 整局 `GAME_OVER`（124 tick）；`action_log` 49 条 LLM（`deepseek-v4-flash`） |
| `bot/run_day4_formal.py` | **10/10** Day4 五项 |

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
| ADR-005 P-01～P-04（推送、锁、tick、定时） | ✅ |

---

## PRD MVP 剩余任务（对照基线）

### §1.2 业务目标

| 优先级 | PRD 目标 | 缺口 | 负责 |
|--------|----------|------|------|
| **P0** | 实时阶段同步，倒计时误差 ≤ 1s | **P-05 已实现**：权威 deadline + WS 递减；验收见 `countdown-observe.py` | B ✅ |
| **P0** | 非法操作不污染状态 | 单测 ✅；Formal 缺系统化 WS 非法用例集 | A/B |
| **P1** | 真人 + AI 混合（S1） | `aiCount` 自动补位、`join` 自动分座未做 | B |
| **P1** | AI JSON 50 次解析率 > 95%，3s 超时 fallback | A-02 待验收；dev `timeout=15s` 与 PRD 3s 不一致 | A |
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
| — | 阶段超时兜底（狼夜 30s 随机刀、投票弃票等） | §4.3.3 | **部分** `PhaseTimeoutHandler` + Mock 兜底；与 PRD 全表逐项对齐待补 |
| — | `NIGHT_WITCH` / `NIGHT_SEER` 阶段跑满 countdown | §4.3.7 | **是**（P-05；无角色时阶段末 fallback） |
| G-03 | SM 发出 `GAME_EVENT` | §4.6 | ✅ `PerceptionLogEvents` → outbound 队列 → WS |
| G-06 | 狼夜超时随机刀非狼 | §4.3.3 | ❌ |

### §4.5 AI（A）— Week2

| 里程碑 | PRD | 状态 |
|--------|-----|------|
| P0.5 狼夜 LLM + `action_log` | §4.5 | ✅ |
| P1 / **A-02** 全角色 LLM + 解析 ≥95% | §8.3 | 能跑局，**未正式验收** |
| — | LLM 3s 超时对齐 PRD | §5 | 逻辑有，配置/指标未对齐 |
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
| AI P95 | < 3s | LLM 冒烟 avg 2.25s，max 4.7s — 未正式验收 |
| 100 局无状态异常 | 压测 | ❌ 报告 v0.1 未出 |
| 信息隔离 | 私密字段不串座 | sync 裁剪 ✅；定向推送压测不足 |

### §8 里程碑

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| §8.1 D5-6 | 压测 20 局 | ❌ |
| §8.3 D4-5 | 全角色 AI + **100 局压测报告** | ❌（C 主责） |
| §8.3 D6-7 | 真人 + Bot 混合局 | 部分（依赖 S1） |
| §8.4 Day7 | 协议/Schema 冻结会 | 文档 ✅；实现对照仍有 gap |

---

## 已完成要点

- PRD v1.0.15、架构文档、ADR 001～005、reference 联调集
- `GameStateMachine` 全阶段 + DeathBus + 遗言 R24
- `AiTurnCoordinator`、`GameViews`、`ActionLogService`、Seat Memory
- 路径 A：`MockGameRunner`、`MockAIFullGameTest`、`InternalGameController`
- LangChain4j + DeepSeek（`AIServiceLlmIntegrationTest`）
- **Gateway P0**：`WsPushService`、`RoomExecutionGuard`、`RoomPhaseTickScheduler`、`ConnectionManager` Bean；`POST /api/room/.../phase-tick`；WS `PHASE_TICK`（dev）
- **P-05**：`PhaseCountdown` + `PhaseTimeoutHandler`；`PHASE_SYNC.countdown` 递减推送（见 [gateway-integration §6](../reference/gateway-integration.md)）
- **room**：建房 / join / ready / start / snapshot
- **Bot**：路径 A `auto_play` / `tick_play`；路径 B `run_day4_formal` + `http_api` / `ws_client`

---

## 待办索引（按负责人）

### 引擎（A）

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| A-02 | 全角色 LLM + JSON 解析 ≥95%（50 次） | §1.2 P1、§8.3 | 待做 |
| G-03 | `GAME_EVENT` 从 SM 发出 | §4.6 | ✅ |
| G-04 | `action_log` MySQL 持久化 | §4.7 | 仅内存 |
| G-06 | 狼夜超时随机刀（SM 兜底） | §4.3.3 | 未做 |
| — | 阶段超时兜底（与 P-05 联动） | §4.3.3、§4.3.7 | **部分**（`PhaseTimeoutHandler`） |
| — | LLM timeout 对齐 PRD 3s | §5 | 配置 15s |

### 网关 / 房间（B）— [ADR-005 §13](../adr/005-gateway-formal-path.md)

| ID | 项 | PRD | 状态 |
|----|-----|-----|------|
| P-01～P-04 | 推送、锁、tick、定时 | §8.5 | **完成** |
| P-05 | `countdown` 权威计时 + 推送 | §1.2 P0、§4.6 | **完成**（2026-05-25） |
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
| `PHASE_SYNC.countdown` | 否 | **是**（权威递减；`countdown-observe.py`） |
| 非法操作不污染状态 | **是**（单测） | **部分**（Day4 基础断言） |
| 12 Bot 同房不串房 | N/A | **是**（Day4） |
| 全角色 LLM 95% 解析 | 单测部分 | **待验收** |
| 100 局压测无异常 | — | **否** |

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

1. **G-06 / 法官兜底细化**：与 PRD §4.3.3 表逐项对齐（P-05 countdown 已落地，见 [gateway-integration §6](../reference/gateway-integration.md)）
2. **A-02**：全角色 LLM + 50 次 JSON ≥95% + timeout 对齐 3s
3. **P-08 / 断线重连**：推送收窄、30s 重连
4. **C**：Formal 路径 100 局压测报告 v0.1
5. **G-04 + P-07**：[persistence-rollout](../reference/persistence-rollout.md) 阶段 1～3

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-05-18 | 精简进度页 |
| v1.4 | 2026-05-25 | B P0；68 测；Bot Day4 |
| v1.5 | 2026-05-25 | 文档重组；PRD v1.0.15 §8.5；Gateway ADR 统一为 005-gateway-formal-path |
| v1.6 | 2026-05-25 | 增补 PRD MVP 全量对照（§1.2～§8 缺口表、非功能、里程碑、负责人索引） |

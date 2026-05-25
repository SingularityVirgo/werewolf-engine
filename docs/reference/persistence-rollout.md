# 持久化落地路线图

| 属性 | 值 |
|------|-----|
| 版本 | v0.1 |
| 日期 | 2026-05-18 |
| 需求真源 | [PRD §4.7](../progress/requirements-mvp-v0.1.md) |
| 架构 | [architecture-design-spec §6](../architecture/architecture-design-spec.md) |

---

## 1. 原则

| 原则 | 说明 |
|------|------|
| **局内真源** | 进行中局：`GameRoomState` + 内存 `action_log` |
| **单写者** | 仅 `GameStateMachine` 修改局况 |
| **避免双写** | Redis 缓存不得与 SM 并行作为 phase 真源 |
| **进程重启** | MVP 允许丢进行中局（PRD 非功能） |

---

## 2. 数据分类

| 数据 | MVP 存储 | 写入时机（目标） |
|------|----------|------------------|
| 用户 | MySQL | 注册后（P1） |
| 房间元数据 | MySQL | `createRoom` |
| `room_player` | MySQL | join / ready |
| 局内状态 | JVM 堆 | `createRoom`～`GAME_OVER` |
| `action_log` | 内存 → MySQL | 每条 append；`GAME_OVER` 整包归档 |
| WS 会话 | Redis | connect / disconnect |
| 阶段缓存（可选） | Redis | **只读镜像**，由 SM 推送后更新 |

---

## 3. 热路径 rollout

### 阶段 0（当前）

- `action_log`：`ActionLogService` 内存 `List`。
- 房间：`GameStateMachine` 内存 `Map<roomId, GameRoomState>`。
- Gateway：内存 `ConnectionManager`。
- `mvn test`：排除 DataSource/Redis（见 [developer-local-setup](../developer-local-setup.md)）。

### 阶段 1 — 房间元数据

- `RoomService.createRoom` / `join` / `ready` 写 `room`、`room_player`。
- 开局仍从内存 SM 读；启动时可选加载 `WAITING` 房。

### 阶段 2 — 对局归档

- `GAME_OVER`：事务写入 `game_record` + `action_log` JSON。
- 应用日志同步结构化行（`roomId`、`playerId`、`requestId`、`phase`）。

### 阶段 3 — Redis 会话

- 实现 PRD §4.7.2 key 前缀。
- Gateway 连接/断开同步；重连读 `werewolf:ws:conn:{roomId}:{playerId}`。

### 阶段 4 — 可选缓存

- `werewolf:game:{roomId}:phase`：仅 SM 提交后 SET；读失败回源 SM。

---

## 4. Redis Key（PRD 摘要）

| Key | 类型 | 用途 |
|-----|------|------|
| `werewolf:ws:conn:{roomId}:{playerId}` | String | sessionId，重连 |
| `werewolf:room:{roomId}:players` | Set | 座位集合 |
| `werewolf:room:{roomId}:alive` | Set | 存活座位 |
| `werewolf:game:{roomId}:phase` | String | 阶段缓存（可选） |
| `werewolf:game:{roomId}:wolf_chat_in_phase` | Flag | R17a 门闩 |

**已冻结**：MVP 不做「无 Redis 降级双路径」。

---

## 5. 与 AI Memory 的关系

- Episodic Memory **不**单独落库；读 `action_log` 投影（[ADR-004](../adr/004-ai-seat-memory.md)）。
- `thinking` 行：归档进 `game_record.action_log`；默认不进他座 Prompt。

---

## 6. 实现归属

| 阶段 | 主要负责 |
|------|----------|
| 1～2 | A（game 归档）+ B（room 表） |
| 3 | B（gateway + Redis） |

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-05-18 | 初稿 |

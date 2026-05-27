# 持久化落地路线图（速查）

| 属性 | 值 |
|------|-----|
| 版本 | v0.2 |
| 日期 | 2026-05-27 |
| 需求真源 | [PRD §4.7](../progress/requirements-mvp-v0.1.md) |
| **决策真源** | **[ADR-007](../adr/007-persistence-redis-mysql.md)**（DDL、Redis TTL、断线时序、验收） |
| 架构 | [architecture-design-spec §6](../architecture/architecture-design-spec.md) |

> 详细规格见 **ADR-007**；本页仅保留阶段 checklist 与原则摘要。

---

## 1. 原则

| 原则 | 说明 |
|------|------|
| **局内真源** | 进行中局：`GameRoomState` + 内存 `action_log` |
| **单写者** | 仅 `GameStateMachine` 修改局况 |
| **避免双写** | Redis 缓存不得与 SM 并行作为 phase 真源 |
| **进程重启** | MVP 允许丢进行中局（PRD 非功能） |

---

## 2. 阶段 checklist

| 阶段 | 开关（Profile） | 交付 | status |
|------|-----------------|------|--------|
| **0（当前）** | — | 内存 room / action_log / ConnectionManager | ✅ |
| **1** | `werewolf.persistence.mysql-room=true` | MySQL `room` / `room_player` 写穿 | ✅ 代码已落地（需 Docker MySQL） |
| **2** | `werewolf.persistence.game-archive=true` | `GAME_OVER` → `game_record` + JSON `action_log` | ✅ 代码已落地 |
| **3** | `werewolf.persistence.redis-session=true` | token + `ws:conn` + 30s grace + 托管 | ✅ 代码已落地 |
| **4（可选）** | `werewolf.persistence.redis-game-cache=true` | phase / alive 只读镜像 | ❌ |

**依赖**：1↔2 可同 Sprint；**3 依赖 1**；**4 依赖 3**。验收条目见 [ADR-007 §7](../adr/007-persistence-redis-mysql.md#7-验收标准)。

---

## 3. 实现归属

| 阶段 | 主要负责 |
|------|----------|
| 1～2 | A（game 归档）+ B（room 表） |
| 3 | B（gateway + Redis） |

分工明细见 [ADR-007 §9](../adr/007-persistence-redis-mysql.md#9-三人分工)。

---

## 4. 相关文档

| 文档 | 用途 |
|------|------|
| [ADR-007](../adr/007-persistence-redis-mysql.md) | MySQL / Redis / 断线 **实现决策** |
| [auth-session.md](auth-session.md) | token、重连（对齐 ADR-007 §5） |
| [ADR-004](../adr/004-ai-seat-memory.md) | Memory 不落库，读 `action_log` 投影 |

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-05-18 | 初稿 |
| v0.2 | 2026-05-27 | 升格 ADR-007 为决策真源；本页压缩为 checklist |
| v0.3 | 2026-05-27 | 阶段 1～2 代码落地（Flyway + 写穿 + 归档） |
| v0.4 | 2026-05-27 | 阶段 3 断线重连（TokenStore、SessionStore、30s grace、AI 托管） |

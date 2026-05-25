# ADR 索引

架构决策记录（ADR）按**领域**分篇。Gateway 见 **[005-gateway-formal-path](005-gateway-formal-path.md)**（决策、实现与选型）。

---

## 阅读顺序（推荐）

| 顺序 | 文档 | 谁读 | 内容 |
|------|------|------|------|
| 0 | [progress/status.md](../progress/status.md) | 全员 | **实现进度真源**（与 PRD 对照） |
| 1 | [PRD §0～2](../progress/requirements-mvp-v0.1.md) | 新人 | 冻结范围、场景 |
| 2a | [001](001-night-skill-pipeline.md) → [002](002-death-bus-and-hunter-flow.md) | A | 夜内管道、死亡总线、猎人 |
| 2b | [003](003-ai-integration.md) → [004](004-ai-seat-memory.md) | A | AI 编排、座位记忆 |
| 2c | **[005](005-gateway-formal-path.md)** | B、C | Gateway 决策 + 实现 + 选型 |
| 3 | [gateway-room-modules](../reference/gateway-room-modules.md) | B、C | 模块图、PRD 实现注记 |
| 4 | [gateway-integration](../reference/gateway-integration.md) | B、C | 双路径联调 |
| 5 | [gateway-room-ws-checklist](../gateway-room-ws-checklist.md) | B | 执行勾选（非架构真源） |

---

## 分篇一览

| ADR | 主题 | 包 | 状态 |
|-----|------|-----|------|
| [001](001-night-skill-pipeline.md) | 夜内技能管道 | `game.night` | 已采纳；与 PRD §4.3.8 对齐 |
| [002](002-death-bus-and-hunter-flow.md) | 死亡总线、猎人流程 | `game.death` / `hunter` | 已采纳；M1～M2 已落地 |
| [003](003-ai-integration.md) | AI 接入与调度 | `game.orchestration` + `ai` | 已采纳；M1～M3 已落地 |
| [004](004-ai-seat-memory.md) | 座位记忆投影 | `game.view` + `ai.memory` | 已采纳；已实现 |
| [005](005-gateway-formal-path.md) | **Formal 路径：推送、tick、串行、选型** | `gateway` + `room` | 已采纳；**P0 已实现**（P1/P2 见文内） |

### 为何不合并 001～004？

- 决策域不同（夜内 / 死亡 / AI / 记忆），评审人与变更频率不同。
- PRD 与 `status.md` 已按 ADR 编号交叉引用；合并单文档会降低可检索性。

## 与 PRD 的关系

| 文档类型 | 角色 |
|----------|------|
| **PRD** | 需求与协议**冻结**基线（改字段须升版本） |
| **ADR** | 设计与实现**决策**（可含 MVP 简化、技术债） |
| **status.md** | 代码与验收**现状**（随实现更新） |
| **reference/** | 联调步骤、包速查（非决策真源） |

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-05-25 | 初稿；Gateway 合并篇；阅读顺序与 status 对齐 |
| 1.1 | 2026-05-25 | 删除旧文件名重定向桩 |

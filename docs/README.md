# werewolf-engine 文档索引

**入口页。** 建议阅读顺序：**进度 → 需求 → ADR → 参考 → 执行清单**。

---

## 1. 先看这两份

| 文档 | 说明 |
|------|------|
| **[progress/status.md](progress/status.md)** | 实现进度、验收结果、待办（**与代码同步的真源**） |
| **[progress/requirements-mvp-v0.1.md](progress/requirements-mvp-v0.1.md)** | 规则、协议、验收基线（**需求冻结真源**） |

PRD 定义「要什么」；status 记录「做到哪」；ADR 记录「为什么这样设计」。

---

## 2. 文档地图

| 类别 | 文档 | 说明 |
|------|------|------|
| **进度** | [status.md](progress/status.md) | 实现状态、P0/P1 待办、测试与 smoke 结果 |
| **需求** | [requirements-mvp-v0.1.md](progress/requirements-mvp-v0.1.md) | MVP **v1.0.17**；§8.5 实现对照（非冻结） |
| **架构** | [architecture-design-spec](architecture/architecture-design-spec.md) · [tech-selection](architecture/tech-selection-feasibility.md) | 系统架构、技术选型 |
| **ADR** | **[adr/README.md](adr/README.md)** | 决策索引与推荐阅读顺序 |
| **参考** | 见下表 | 包结构、联调、鉴权、测试 |
| **执行清单** | [gateway-room-ws-checklist](gateway-room-ws-checklist.md) | B 侧勾选（非架构真源） |
| **本地环境** | [developer-local-setup](developer-local-setup.md) | JDK、Docker、API Key |

### ADR 一览（详见 [adr/README.md](adr/README.md)）

| ADR | 主题 |
|-----|------|
| [001](adr/001-night-skill-pipeline.md) | 夜内管道 |
| [002](adr/002-death-bus-and-hunter-flow.md) | 死亡总线、猎人 |
| [003](adr/003-ai-integration.md) | AI 接入与调度 |
| [004](adr/004-ai-seat-memory.md) | 座位记忆 |
| **[005](adr/005-gateway-formal-path.md)** | **Gateway Formal：推送、tick、选型** |
| [006](adr/006-mock-vs-llm-intent.md) | Mock vs LLM、6s/重试、排查 |

### 参考文档（reference/）

| 文档 | 读者 | 说明 |
|------|------|------|
| [code-modules.md](reference/code-modules.md) | 全员 | 包结构速查 |
| [gateway-room-modules.md](reference/gateway-room-modules.md) | B、C | 模块图、PRD 实现注记 |
| [gateway-integration.md](reference/gateway-integration.md) | B、C、A | Internal vs Formal 双路径 |
| [auth-session.md](reference/auth-session.md) | B | token、重连（P2） |
| [persistence-rollout.md](reference/persistence-rollout.md) | A、B | MySQL/Redis 分阶段 |
| [testing-strategy.md](reference/testing-strategy.md) | 全员 | 单测 / smoke / Day4 |
| [bot-load-test.md](reference/bot-load-test.md) | C | Bot 压测模板 |
| **[frontend-ui-spec.md](reference/frontend-ui-spec.md)** | **C、前端** | **Web UI 线框与交互基线（三屏、6+6 布局）** |

---

## 3. 谁读什么

| 角色 | 推荐阅读顺序 |
|------|----------------|
| **新人** | 本页 → [status](progress/status.md) → PRD §0～2 → 架构 §1～3 → [code-modules](reference/code-modules.md) |
| **A（game/ai）** | [status](progress/status.md) → PRD §3～4.5 → ADR [001](adr/001-night-skill-pipeline.md)～[004](adr/004-ai-seat-memory.md) |
| **B（gateway）** | [status](progress/status.md) → PRD §4.2、§4.6～4.7、§8.5 → **[ADR-005](adr/005-gateway-formal-path.md)** → [gateway-room-modules](reference/gateway-room-modules.md) → [gateway-integration](reference/gateway-integration.md) → [checklist](gateway-room-ws-checklist.md) |
| **C（联调脚本）** | [status](progress/status.md) → [gateway-integration](reference/gateway-integration.md) → [bot-load-test](reference/bot-load-test.md) → [scripts/README](../scripts/README.md) |

---

## 4. 三类文档如何分工

```text
PRD（冻结契约）  ←──对照──  status（实现现状）
        ↑                      ↑
        └── 细化决策 ── ADR（设计 + 选型 + MVP 简化）
                      └── reference（怎么联调、包在哪）
```

Cursor 规则：[werewolf-engine-context.mdc](../.cursor/rules/werewolf-engine-context.mdc)

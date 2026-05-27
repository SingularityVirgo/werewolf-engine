# 文档体系整理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize `docs/checklists/`, sync documentation with code via `status.md`, unify user-facing copy to「预女猎愚」, and fix broken root `README.md`—without changing frozen API enums.

**Architecture:** Keep PRD / ADR / `status.md` as the three-tier truth model; move B-side checklist to `docs/checklists/gateway-room-ws.md` with a redirect stub at the old path; refresh checklist task statuses from `status.md` + code; terminology changes are display-only (`STANDARD_12_PRYH_IDIOT` unchanged).

**Tech Stack:** Markdown docs, PowerShell/`rg` verification, optional `frontend` copy edits.

**Spec:** [2026-05-27-documentation-audit-design.md](../specs/2026-05-27-documentation-audit-design.md)

---

## File map

| Action | Path |
|--------|------|
| Create | `docs/checklists/gateway-room-ws.md` |
| Replace (stub) | `docs/gateway-room-ws-checklist.md` |
| Modify | `docs/README.md`, `docs/progress/status.md`, `docs/architecture/tech-selection-feasibility.md` |
| Modify | `docs/reference/gateway-integration.md`, `gateway-room-modules.md`, `testing-strategy.md` |
| Modify | `docs/adr/README.md`, `docs/progress/requirements-mvp-v0.1.md`, `docs/reference/frontend-ui-spec.md` |
| Modify | `PRODUCT.md`, `DESIGN.md` (only if 预女猎白 appears—currently DESIGN.md does not) |
| Modify | `frontend/src/constants/boardTypes.ts`, `frontend/src/pages/HomePage.tsx` |
| Modify | `src/main/java/com/werewolfengine/room/BoardTypes.java` |
| Rewrite | `README.md` (repo root, UTF-8) |

---

### Task 1: Migrate gateway checklist

**Files:**
- Create: `docs/checklists/gateway-room-ws.md`
- Modify: `docs/gateway-room-ws-checklist.md`

- [ ] **Step 1: Copy checklist to new location**

Copy `docs/gateway-room-ws-checklist.md` → `docs/checklists/gateway-room-ws.md`.

Update the metadata block at top:

```markdown
| 版本 | v0.2 |
| 日期 | 2026-05-27 |
| 关联文档 | [status](../progress/status.md)、[ADR-005](../adr/005-gateway-formal-path.md)、[gateway-room-modules](../reference/gateway-room-modules.md)、[developer-local-setup](../developer-local-setup.md) |
```

- [ ] **Step 2: Remove §1「当前进展」and add status link**

Delete lines from `## 1. 当前进展` through the table ending before `## 2. 联调层目标`.

Insert immediately after the blockquote `> **定位**`…:

```markdown
**实现进度**（模块摘要、验收、缺口）见 **[progress/status.md](../progress/status.md)**，本页仅保留可勾选任务。
```

Renumber sections: old §2→§1, §3→§2, §4→§3, §5→§4, §6→§5, §7→§6, §8→§7, §9→§8.

- [ ] **Step 3: Refresh §2（原 §3）task statuses**

In `### 2.1 gateway` table, set:

| ID | 状态 |
|----|------|
| G-03 | 部分完成：内存 `ConnectionManager`；**心跳未实现** |
| G-04 | 已完成：`MessageRouter` 接 game；`CHAT_MESSAGE`/`WOLF_CHAT` 经 `ChatMessageService` |
| G-06 | 未开始 |
| G-07 | 未开始 |

In `### 2.2 room` table:

| ID | 状态 |
|----|------|
| R-02 | **已完成**：不传 `seatId` 时自动分座 |
| R-05 | **已完成**：`DELETE /api/room/{id}` |
| R-06 | **已完成**：`GET` 含 `seats[]` |

In `### 2.4 消息闭环` table:

| ID | 状态 |
|----|------|
| M-05 | **已完成**：`PerceptionLogEvents` → WS `GAME_EVENT` |

In `## 7. 后续任务` table:

| ID | 状态 |
|----|------|
| N-03 | **已完成**（`WsPushService` + `RoomPhaseTickScheduler`） |
| N-04 | **已完成**（自动分座） |

Add after `## 1. 联调层目标` a short note:

```markdown
Formal Bot 验收：**Day4 10/10**（`scripts/formal/run_day4_formal.py`，见 [status](../progress/status.md)）。
```

- [ ] **Step 4: Replace old file with redirect stub**

Overwrite `docs/gateway-room-ws-checklist.md` with:

```markdown
# gateway / room / WS 联调清单（已迁移）

本清单已迁至 **[checklists/gateway-room-ws.md](checklists/gateway-room-ws.md)**。

实现进度见 [progress/status.md](progress/status.md)。
```

- [ ] **Step 5: Verify new file exists**

Run:

```powershell
Test-Path docs/checklists/gateway-room-ws.md
Select-String -Path docs/checklists/gateway-room-ws.md -Pattern "## 1\. 当前进展"
```

Expected: `True` and **no matches** for「当前进展」section title.

---

### Task 2: Update cross-references to checklist

**Files:**
- Modify: `docs/README.md`
- Modify: `docs/adr/README.md`
- Modify: `docs/reference/gateway-room-modules.md`
- Modify: `docs/reference/gateway-integration.md`
- Modify: `docs/reference/testing-strategy.md`

- [ ] **Step 1: Replace paths in all five files**

| File | Old | New |
|------|-----|-----|
| `docs/README.md` (2 places) | `gateway-room-ws-checklist.md` | `checklists/gateway-room-ws.md` |
| `docs/adr/README.md` | `../gateway-room-ws-checklist.md` | `../checklists/gateway-room-ws.md` |
| `docs/reference/gateway-room-modules.md` | `../gateway-room-ws-checklist.md` | `../checklists/gateway-room-ws.md` |
| `docs/reference/gateway-integration.md` | `../gateway-room-ws-checklist.md` | `../checklists/gateway-room-ws.md` |
| `docs/reference/testing-strategy.md` | `../gateway-room-ws-checklist.md` | `../checklists/gateway-room-ws.md` |

In `testing-strategy.md`, update anchor text if section numbers changed: `§5` → `§4`（联调验收，renumber 后）.

- [ ] **Step 2: Verify no stale primary links**

Run:

```powershell
rg "gateway-room-ws-checklist" docs --glob "*.md"
```

Expected: only `docs/gateway-room-ws-checklist.md` (redirect stub) may mention the old filename in body text; all other `docs/**/*.md` should use `checklists/gateway-room-ws`.

---

### Task 3: Fix `status.md` and tech-selection header

**Files:**
- Modify: `docs/progress/status.md`
- Modify: `docs/architecture/tech-selection-feasibility.md`

- [ ] **Step 1: Fix LLM timeout contradiction in status**

In `docs/progress/status.md` line ~64, replace the P1 AI JSON row **缺口** cell:

From:

```markdown
| ✅ A-02；PRD v1.0.17 已升；**代码待** `llmTimeoutSeconds=6` + 重试循环 | A |
```

To:

```markdown
| ✅ A-02 + `llm-timeout-seconds=6`、`max-llm-retries=2`（`AiProperties` + `AIServiceRetryTest`） | A |
```

- [ ] **Step 2: Add implementation note (optional, after「结论」table)**

Insert before `---` following the one-line conclusion:

```markdown
**实现注记：** 夜内 `RoleSkillHandler` 尚未拆为独立类；逻辑在 `NightSkillPipeline` / `NightActions`（见 [ADR-001](../adr/001-night-skill-pipeline.md) P1）。
```

- [ ] **Step 3: Update tech-selection metadata**

In `docs/architecture/tech-selection-feasibility.md` header table:

```markdown
| 版本 | v0.1.6 |
| 日期 | 2026-05-27 |
| 关联文档 | [PRD](../progress/requirements-mvp-v0.1.md)（**v1.0.17**）、[architecture-design-spec.md](./architecture-design-spec.md)、[developer-local-setup.md](../developer-local-setup.md) |
```

Add to §1.2 conclusion table footnote or a one-line note under §1.2:

```markdown
> Week1「协议 Day7 冻结」已于 v1.0.0 完成；下文保留历史决策上下文。
```

- [ ] **Step 4: Verify status fix**

Run:

```powershell
Select-String -Path docs/progress/status.md -Pattern "代码待.*llmTimeout"
```

Expected: **no matches**.

---

### Task 4: Unify terminology to「预女猎愚」

**Files:**
- Modify: `PRODUCT.md`
- Modify: `docs/reference/frontend-ui-spec.md`
- Modify: `docs/progress/requirements-mvp-v0.1.md`
- Modify: `frontend/src/constants/boardTypes.ts`
- Modify: `frontend/src/pages/HomePage.tsx`
- Modify: `src/main/java/com/werewolfengine/room/BoardTypes.java`

- [ ] **Step 1: Replace all「预女猎白」with「预女猎愚」**

Run first to list targets:

```powershell
rg "预女猎白" PRODUCT.md docs frontend/src -g "*.{md,ts,tsx}"
```

Apply replacements:

| File | Example after |
|------|----------------|
| `PRODUCT.md` | `预女猎愚：4 狼 + 4 民 + 预/女/猎/愚` |
| `frontend-ui-spec.md` | all wireframe/table copy |
| `requirements-mvp-v0.1.md` ~L1182 | `（预女猎愚 · 12 人）` |
| `boardTypes.ts` | `name: '预女猎愚 · 12 人标准场'` |
| `HomePage.tsx` | `十二人围坐，预女猎愚标准局。` |

- [ ] **Step 2: Add BoardTypes Javadoc**

In `src/main/java/com/werewolfengine/room/BoardTypes.java`, above the class:

```java
/**
 * MVP board types. {@link #STANDARD_12_PRYH_IDIOT} = 预女猎 + 愚者（展示名「预女猎愚」）.
 */
```

- [ ] **Step 3: Verify terminology**

Run:

```powershell
rg "预女猎白" PRODUCT.md docs frontend/src src/main/java -g "*.{md,ts,tsx,java}"
```

Expected: **no matches**.

Run (enum preserved):

```powershell
rg "STANDARD_12_PRYH_IDIOT" src frontend -g "*.{java,ts}"
```

Expected: matches remain (unchanged API value).

---

### Task 5: Refresh `gateway-integration.md` script row

**Files:**
- Modify: `docs/reference/gateway-integration.md`

- [ ] **Step 1: Update path B scripts row**

Replace table row for **C 脚本** under path B:

```markdown
| **C 脚本** | `scripts/formal/run_day4_formal.py`、`formal_path_smoke.py`、`formal_llm_smoke.py`（`scripts/lib/bot_player.py`） |
```

Remove「待对齐」wording from path B column.

- [ ] **Step 2: Bump changelog**

Add to `## 变更记录`:

```markdown
| v0.5 | 2026-05-27 | Formal 脚本已对齐 `bot_player`；checklist 迁至 checklists/ |
```

---

### Task 6: Rewrite root `README.md` and extend `docs/README.md`

**Files:**
- Rewrite: `README.md`
- Modify: `docs/README.md`

- [ ] **Step 1: Write UTF-8 root README**

Replace entire `README.md` with:

```markdown
# werewolf-engine

**AI 狼人杀** — 多智能体协作与博弈的 Agent Team 实战：Java 21 + Spring Boot 4 服务端权威状态机，12 人预女猎愚标准局，LangChain4j + DeepSeek 驱动 AI 座位。

## 文档

**[docs/README.md](docs/README.md)** — 需求（PRD）、架构、ADR、进度与联调索引。

| 快速入口 | |
|----------|--|
| [实现进度](docs/progress/status.md) | 与代码同步的真源 |
| [需求基线 PRD v1.0.17](docs/progress/requirements-mvp-v0.1.md) | 冻结契约 |
| [本地环境](docs/developer-local-setup.md) | JDK、Docker、API Key |

## 快速启动

```bash
./mvnw spring-boot:run
```

详见 [developer-local-setup.md](docs/developer-local-setup.md)。

## 代码布局

```text
src/main/java/com/werewolfengine/   # gateway / room / game / ai / message
scripts/formal/                     # Formal 路径联调与 Day4 验收
docs/                               # 文档
frontend/                           # Web UI（对局控制台）
```
```

Save as **UTF-8 without BOM**.

- [ ] **Step 2: Add Frontend section to docs/README**

After `### 参考文档（reference/）` table, insert:

```markdown
### 前端 / 产品（仓库根目录 + reference）

| 文档 | 读者 | 说明 |
|------|------|------|
| [PRODUCT.md](../PRODUCT.md) | 产品、前端 | 用户场景与成功标准 |
| [DESIGN.md](../DESIGN.md) | 前端 | 视觉令牌（烛火法庭） |
| [frontend-ui-spec.md](reference/frontend-ui-spec.md) | 前端、C | 三屏线框、WS 交互、6+6 布局 |
```

- [ ] **Step 3: Verify README encoding**

Run:

```powershell
Get-Content README.md -Encoding UTF8 | Select-Object -First 3
```

Expected: readable Chinese, no spaced UTF-16 characters.

---

### Task 7: Final acceptance

- [ ] **Step 1: Run acceptance greps**

```powershell
rg "预女猎白" PRODUCT.md docs frontend/src src/main/java -g "*.{md,ts,tsx,java}"
rg "## 1\. 当前进展" docs/checklists/gateway-room-ws.md
rg "代码待.*llmTimeout" docs/progress/status.md
rg "gateway-room-ws-checklist\.md" docs --glob "*.md" | Where-Object { $_ -notmatch "gateway-room-ws-checklist.md:" }
```

Expected:

1. No「预女猎白」in user copy paths.
2. No「当前进展」in new checklist.
3. No LLM「代码待」in status.
4. Cross-links use `checklists/gateway-room-ws` except redirect stub file.

- [ ] **Step 2: Manual spot-check**

Open `docs/checklists/gateway-room-ws.md` §2 room table: R-02 / R-05 / R-06 show 已完成.

Open `docs/README.md`: Frontend 三链可点击。

---

## Spec coverage checklist

| Spec § | Task |
|--------|------|
| §2 目录结构 | Task 1, 2 |
| §3 预女猎愚 | Task 4 |
| §4.1 status | Task 3 |
| §4.2 checklist | Task 1 |
| §4.3 tech-selection | Task 3 |
| §4.4 gateway-integration | Task 5 |
| §4.4 根 README | Task 6 |
| §4.5 docs/README Frontend | Task 6 |
| §6 验收 | Task 7 |

---

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-05-27 | 初稿；对应已批准 design spec |

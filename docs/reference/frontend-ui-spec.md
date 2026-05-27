# 前端 UI 规格（原型基线）

| 属性 | 值 |
|------|-----|
| 版本 | v0.3 |
| 日期 | 2026-05-27 |
| 设计 North Star | **烛火法庭**（见 [DESIGN.md](../../DESIGN.md)） |
| 产品上下文 | [PRODUCT.md](../../PRODUCT.md) |
| 协议基线 | [requirements-mvp-v0.1.md](../progress/requirements-mvp-v0.1.md) §4.6（冻结） |
| 实现进度 | [status.md](../progress/status.md) §前端 |

本文档是 Web UI 的**唯一线框与交互真源**。实现以 `frontend/` 为准；与 PRD 协议冲突时以 PRD 为准。

---

## 0. Impeccable 设计约束（全站）

**场景句（Home）：** 玩家深夜推开一间石砌牌室的门，烛火在壁龛里摇晃，桌上十二把空椅，空气里有 wax 与旧木头的气味。

**场景句（Game）：** 同一间牌室，灯更暗；玩家盯左右两列座位与中央法官区，等播报、等轮到自己发言。

| 原则 | 要求 |
|------|------|
| Register | **Home / Setup = brand**（氛围即产品）；**Game = product**（3 秒内读懂阶段与操作） |
| 色彩策略 | Home：**Committed**（深褐墨绿底 + 烛金 30% 点缀）；Game：**Restrained**（深色底 + 烛金 ≤10%，血红仅危险/死亡） |
| 禁止 | 紫粉渐变 hero、gradient text、glass 默认卡片、左侧色条 accent、emoji 图标系统、全屏粒子循环 |
| 字体 | Display：Noto Serif SC / Cinzel；UI：IBM Plex Sans SC；Mono：IBM Plex Mono（座位号、倒计时） |
| 动效 | ease-out-quart；烛火微动仅 Home；`prefers-reduced-motion` 关闭装饰动画 |
| 文案 | 法官播报口吻；无 em dash；无「AI 狼人杀之旅」式营销 |

---

## 1. 信息架构

```mermaid
flowchart LR
  Home["HomePage"]
  Setup["SetupPage"]
  Game["GamePage"]
  Home -->|"开始游戏"| Setup
  Setup -->|"创建成功"| Game
  Game -->|"退出"| Home
```

| 屏 | 路由状态 | 职责 |
|----|----------|------|
| `HomePage` | `home` | 中世纪奇幻氛围；单一 CTA |
| `SetupPage` | `setup` | 选板 + 创建房间 |
| `GamePage` | `game` | 12 人对局（含等待房） |

MVP **不提供**「加入他人房间」入口（HTTP `join` 保留给 Bot，P1 再做 UI）。

---

## 2. HomePage（brand register）

### 2.1 目标

建立「烛火法庭」世界感，引导进入 Setup；**不是** SaaS landing（无三特性卡、无 testimonial、无假数据）。

### 2.2 布局

```
┌────────────────────────────────────────┐
│  [薄雾 vignette 全屏，非粒子]            │
│                                        │
│         狼 人 杀                        │  ← display, 烛金
│    十二人 · 预女猎愚 · 标准场            │  ← body, mist 色
│                                        │
│         [ 进入牌室 ]                      │  ← btn-primary
│                                        │
│  脚注一行（可选）：werewolf-engine       │
└────────────────────────────────────────┘
```

### 2.3 视觉规格

| 元素 | Token / 类 | 说明 |
|------|------------|------|
| 背景 | `abyss` + radial 薄雾 | 深褐偏绿，非纯黑 |
| 标题 | `font-display text-display text-ember` | 字距略宽 |
| 副标题 | `text-body text-mist` | 一行，不重复标题 |
| CTA | `.btn-primary` | 文案「进入牌室」，非「开始 AI 之旅」 |
| 装饰 | 可选 SVG 烛台/拱窗 silhouette | 低对比，不抢 CTA |

### 2.4 动效

- 入场：`animate-fade-in` 400ms ease-out-quart
- 烛火：CSS `opacity` 微动 3s（`motion-reduce:animate-none`）
- 禁止：bounce、全屏 parallax 粒子

### 2.5 交互

| 操作 | 结果 |
|------|------|
| 点击「进入牌室」 | `App` 切至 `setup` |
| 键盘 Enter（焦点在 CTA） | 同上 |

---

## 3. SetupPage（brand → product 过渡）

### 3.1 目标

选择板子并创建房间；MVP 仅 **预女猎愚** 可点，其余占位 disabled。

### 3.2 布局

```
┌────────────────────────────────────────┐
│  ← 返回        选择板子                  │
├────────────────────────────────────────┤
│  ┌─ 预女猎愚 · 12 人标准场 ─── [已选] ─┐ │
│  │ 4 狼 · 4 民 · 预/女/猎/愚            │ │
│  │ [ 创建房间 ]                         │ │
│  └────────────────────────────────────┘ │
│  ┌─ 守卫局 · 12 人 ─────── [即将开放] ─┐ │  disabled
│  └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

### 3.3 板子枚举（与 API 对齐）

| `boardType` | 显示名 | MVP |
|-------------|--------|-----|
| `STANDARD_12_PRYH_IDIOT` | 预女猎愚 · 12 人标准场 | ✅ 唯一可选 |
| （预留） | 守卫局等 | disabled |

角色构成与 PRD §3.1 一致：4 狼、4 村民、1 预言家、1 女巫、1 猎人、1 愚者。

### 3.4 创建请求

```http
POST /api/room
Content-Type: application/json

{
  "boardType": "STANDARD_12_PRYH_IDIOT",
  "aiCount": 11
}
```

| 字段 | 默认 | 说明 |
|------|------|------|
| `boardType` | `STANDARD_12_PRYH_IDIOT` | 未知值 → HTTP 400 |
| `aiCount` | `11` | 1 真人（房主 #1）+ 11 AI |
| `hostUserId` | 随机生成 | 前端本地 userId |

成功后：`App` 带 `{ roomId, seatId: 1, userId, isOwner: true }` 进入 `GamePage`。

### 3.5 视觉

- 面板：`.panel-parchment`（羊皮纸感，深边石框）
- 选中板：全边框 `border-ember/50`，非左侧色条
- 错误：一行 `text-blood`，inline，不用 modal

---

## 4. GamePage（product register）

### 4.1 目标

对局控制台：3 秒内回答「什么阶段、剩多少秒、我能做什么、谁在发言」。

### 4.2 线框（冻结）

```
┌──────────────────────────────────────────────────────────┐
│ 顶栏：阶段 · 第 N 轮 · 倒计时 · 身份 · [准备|开始|退出]   │
├─────────┬────────────────────────────┬─────────────────┤
│  #1      │                            │  #7             │
│  #2      │      中间主区               │  #8             │
│  #3      │   (phase-driven)           │  #9             │
│  #4      │                            │  #10            │
│  #5      │                            │  #11            │
│  #6      │                            │  #12            │
├─────────┴────────────────────────────┴─────────────────┤
│ 底栏：ActionPanel（canAct / canVote / 角色夜）          │
└──────────────────────────────────────────────────────────┘
```

- 左列：座位 **#1–#6** 自上而下
- 右列：座位 **#7–#12** 自上而下
- 中间：最小宽度 320px，flex-1
- 底栏：全宽，不遮挡座位列

### 4.3 顶栏 PhaseDisplay

| 区 | 内容 | 数据源 |
|----|------|--------|
| 左 | 阶段 badge + 轮次 + 夜/日标签 | `PHASE_SYNC.currentPhase`, `round` |
| 右 | 倒计时 + 我的身份 | `countdown`, `yourRole` |

### 4.4 中间主区 CenterStage（阶段驱动）

| 阶段族 | 组件 | 展示 | 输入 |
|--------|------|------|------|
| `WAITING` | `WaitingCenter` | 房间 ID、已准备人数提示 | 无（顶栏准备/开始） |
| `NIGHT_*`, `NIGHT_START` | `NightAnnouncer` | 阶段文案 + 事件流（F-03） | 无 |
| `NIGHT_DEATH_ANNOUNCE`, `EXILE_DEATH_ANNOUNCE` | `NightAnnouncer` | 死讯宣读 | 无 |
| `LAST_WORDS` | `LastWordsCenter` | 「#N 遗言中」 | 仅 `currentSpeakerId === mySeatId` 可输入 |
| `DAY_DISCUSS` | `PublicChat` | 公频消息 | 仅当前发言人可 `SPEAK` |
| `DAY_VOTE`, `VOTE_RESULT` | `PublicChat` + 提示条 | 历史公频 + 「请投票」 | 底栏 ActionPanel |
| `CHECK_WIN`, `GAME_OVER` | `NightAnnouncer` / 结算 overlay | 胜负文案 | 无 |

**狼频道**：不在公频混排。狼人且 `wolfChatInPhase` 时，中间区顶 **Tab：公频 | 狼频道**（F-01/F-02）。

### 4.5 公频发言规则（R13）

| 条件 | 行为 |
|------|------|
| `phase === DAY_DISCUSS` 且 `currentSpeakerId === mySeatId` | 底栏 `DayDiscussAction`：`SPEAK`（公频输入）+ `SKIP_SPEAK` |
| 其他存活玩家 | 只读消息流；对局由 `usePhaseTick` 调 `POST /api/room/{id}/phase-tick` 推进 AI |
| `LAST_WORDS` 且 `currentSpeakerId === mySeatId` | 遗言输入（同 `SPEAK` 动作） |

数据源：`PHASE_SYNC.currentSpeakerId`, `speakDirection`, `speakAnchorSeat`（方向 UI P1 可显）。

### 4.6 座位 SeatCard 状态

| 状态 | 条件 | 视觉（色 + 文案，不单靠颜色） |
|------|------|-------------------------------|
| 默认 | 存活 | 石纹底 `stone-surface`，#n mono |
| 我的座位 | `seatId === mySeatId` | `ring-2 ring-ember` + 底部「我」 |
| 选中目标 | 可选中阶段 + 已点选 | `ring-2 ring-blood` |
| **发言中** | `currentSpeakerId === seatId` | `ring-2 ring-ember animate-speaking` + 标签「发言中」 |
| 已准备 | `ready && alive` | 右上角烛点 |
| 死亡 | `!alive` | 灰化 + 遮罩「出局」 |

### 4.7 底栏 ActionPanel

与现逻辑一致：`canAct` / `canVote` / 角色阶段渲染子面板；位置固定底部，宽度 100%。

| 组件 | 阶段 | 说明 |
|------|------|------|
| `DayDiscussAction` | `DAY_DISCUSS` + `canAct` | 跳过发言 `SKIP_SPEAK` |
| `usePhaseTick` | 玩家阶段（非 `WAITING`） | 约 1.5s 轮询 `phase-tick`，驱动 AI 座位行动 |
| `NightWolfAction` 等 | 对应夜/票/猎 | 见 `ActionPanel.tsx`；**F-13** 狼夜 KILL 反馈仍待做（[status §前端](../progress/status.md)） |

---

## 5. WebSocket / HTTP 字段对照

### 5.1 CHAT_BROADCAST（F-01 映射）

后端 payload：

```json
{ "scope": "ALL|WEREWOLF", "playerId": 3, "content": "…", "phase": "DAY_DISCUSS", "round": 1 }
```

前端归一化：

```ts
{ seatId: payload.playerId, content, isWolfChat: payload.scope === 'WEREWOLF' }
```

### 5.2 PHASE_SYNC（发言）

| 字段 | UI 用途 |
|------|---------|
| `currentSpeakerId` | 座位「发言中」高亮；公频输入门控 |
| `speakDirection` | P1：箭头提示发言方向 |
| `speakAnchorSeat` | P1：首日锚点展示 |
| `countdown` | 顶栏倒计时 |
| `canAct` / `canVote` | 底栏 ActionPanel |
| `wolfChatInPhase` | 狼频道 Tab 可见性 |

### 5.3 GAME_EVENT（F-03）

```json
{ "eventType": "NIGHT_DEATHS|EXILE_ANNOUNCED|…", "data": { … } }
```

映射为中文法官句写入 `gameLog`，展示于 `NightAnnouncer`（非 raw JSON）。

### 5.4 GAME_OVER（F-05）

优先 WS `GAME_OVER`；fallback `PHASE_SYNC.currentPhase === GAME_OVER`。

---

## 6. 组件文件映射

| 规格组件 | 文件 |
|----------|------|
| HomePage | `frontend/src/pages/HomePage.tsx` |
| SetupPage | `frontend/src/pages/SetupPage.tsx` |
| GamePage | `frontend/src/components/GamePage.tsx` |
| GameTableSideLayout | `frontend/src/components/GameBoard/GameTableSideLayout.tsx` |
| CenterStage | `frontend/src/components/GameBoard/CenterStage.tsx` |
| SeatCard | `frontend/src/components/GameBoard/SeatCard.tsx` |
| PhaseDisplay | `frontend/src/components/PhaseBar/PhaseDisplay.tsx` |
| ActionPanel | `frontend/src/components/ActionPanel/*` |

---

## 7. MVP 外（P1+）

- 加入他人房间 UI
- 弧形牌桌（仍 12 座，旋转视角）
- 多板子后端规则切换
- 移动端触控 44px 优化
- `speakDirection` 可视化

---

## 8. 验收清单

- [x] Home → Setup → Create → Game 可跑通
- [x] 左右各 6 座，编号 1–6 / 7–12
- [x] 白天中间为公频；夜晚中间为宣读/阶段（狼夜含狼频道 Tab）
- [x] `currentSpeakerId` 座位高亮 + 输入门控
- [x] `boardType` 请求/响应一致（`STANDARD_12_PRYH_IDIOT`）
- [x] 无 SaaS hero / 紫渐变 / emoji 图标堆叠
- [x] `DayDiscussAction` + `usePhaseTick`（2026-05-27）
- [ ] **F-13** 狼夜击杀成功/失败 Toast 与 `WAITING_WOLF_CONSENSUS` 展示

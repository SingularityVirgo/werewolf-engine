---
name: Werewolf Engine UI
description: 烛火法庭 — 中世纪奇幻 Home + 左右列对局控制台
colors:
  abyss: "#0d1210"
  stone: "#1a1814"
  stone-surface: "#242019"
  stone-border: "#3d3629"
  parchment: "#2a251c"
  parchment-light: "#3a3228"
  ember: "#c9a227"
  ember-muted: "#8a7020"
  mist: "#8a9a8e"
  blood: "#a33030"
  blood-muted: "#6b2020"
  wolf: "#6b1515"
  villager: "#2d4a28"
  seer: "#1a3348"
  witch: "#3a1a48"
  hunter: "#4a3018"
  idiot: "#4a4818"
  text-primary: "#e8e4dc"
  text-secondary: "#a8a090"
  text-muted: "#6a6458"
typography:
  display:
    fontFamily: "Noto Serif SC, Cinzel, Georgia, serif"
    fontSize: "clamp(2rem, 5vw, 3rem)"
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: "0.04em"
  title:
    fontFamily: "IBM Plex Sans SC, Noto Sans SC, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.35
  body:
    fontFamily: "IBM Plex Sans SC, Noto Sans SC, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "IBM Plex Sans SC, Noto Sans SC, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
    letterSpacing: "0.08em"
  mono:
    fontFamily: "IBM Plex Mono, ui-monospace, monospace"
    fontSize: "0.875rem"
    fontWeight: 500
rounded:
  sm: "4px"
  md: "8px"
  lg: "12px"
components:
  button-primary:
    backgroundColor: "{colors.ember}"
    textColor: "{colors.abyss}"
  panel-parchment:
    backgroundColor: "{colors.parchment}"
    borderColor: "{colors.stone-border}"
  seat-tile:
    backgroundColor: "{colors.stone-surface}"
    borderColor: "{colors.stone-border}"
---

## Overview

**Creative North Star: 「烛火法庭」(Candlelit Tribunal)**

一间 **石砌牌室**：Home 有薄雾与烛光（brand）；Game 更暗，左右两列十二座，中央法官区播报死讯或公频发言（product）。

**Implementation map:** tokens 在 `frontend/tailwind.config.js` 与 `frontend/src/index.css`。线框见 [docs/reference/frontend-ui-spec.md](docs/reference/frontend-ui-spec.md)。

### 双 Register

| 页面 | Register | 色彩策略 | 密度 |
|------|----------|----------|------|
| Home, Setup | brand | Committed：深底 + 烛金点缀 | 低，留白与氛围 |
| Game | product | Restrained：烛金 ≤10%，血红仅语义 | 高，信息优先 |

## Colors

**禁止** `#000` / `#fff`；中性向墨绿褐 tint。

| Token | 用途 |
|-------|------|
| `abyss` | Home 背景、Game 画布 |
| `stone` / `stone-surface` | Game 面板、座位底 |
| `parchment` | Setup 板子卡、Home 装饰区 |
| `ember` | CTA、我的座位、发言高亮、主 accent |
| `blood` | 选中目标、危险操作、死亡语义 |
| `mist` | Home 副文案、muted 信息 |

角色色（`wolf/seer/...`）**仅** PhaseDisplay、RoleBadge，不扩散全页。

## Typography

- **Display（Noto Serif SC / Cinzel）：** Home 标题、Setup 页眉、结算标题
- **UI（IBM Plex Sans SC）：** 阶段、按钮、聊天
- **Mono：** 房间 ID、座位号 `#3`、倒计时

Hierarchy：阶段 title semibold；座位号 label + mono；聊天 body。

## Layout

**Game（冻结）：** 顶栏 + 三列（左 6 座 | 中间 | 右 6 座）+ 底栏操作。

**废弃 MVP 目标：** 4×3 `seat-grid`、右侧边栏聊天。

## Elevation

- ** tonal layering**，默认无 box-shadow
- 选中/发言：`ring-2 ring-ember` 或 `ring-blood`
- 禁止默认 glass / backdrop-blur

## Components

### Home

- 全屏 `bg-abyss`，radial 薄雾 vignette（CSS gradient，非 gradient text）
- 单 CTA `.btn-primary`「进入牌室」
- 可选 `.candle-glow` 装饰（opacity 动画，motion-reduce 关闭）

### Setup

- `.panel-parchment` 板子卡；选中 = 全边框 ember，非 side-stripe
- disabled 板：opacity-40，文案「即将开放」

### Game Seat

- `.seat-tile` 石纹底；发言中 `.seat-speaking`（ember ring + 标签）
- 死亡：灰化 + 「出局」遮罩

### CenterStage

- 夜：`NightAnnouncer` 法官区，muted 事件列表
- 日：`PublicChat` 消息流 + 条件输入
- 狼 Tab：仅狼可见，subtle `wolf/10` 背景 tint

## Motion

- 入场 fade-in：`cubic-bezier(0.16, 1, 0.3, 1)`
- 发言脉动：opacity/ring，非 layout 动画
- GSAP（可选 P1）：阶段 crossfade
- `prefers-reduced-motion`：关闭烛火与脉动

## Do's and Don'ts

### Do

- 先读 `frontend-ui-spec.md` 与 PRODUCT.md
- Home brand / Game product 分开实现
- 6+6+center 布局
- `currentSpeakerId` 驱动发言 UI

### Don't

- SaaS hero、三列 feature grid
- 紫粉渐变、gradient text
- emoji 图标系统
- 4×3 网格作为对局主布局
- Modal 首选交互

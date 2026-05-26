# Product

## Register

**Dual register:** Home / Setup = **brand**；Game = **product**（见 [docs/reference/frontend-ui-spec.md](docs/reference/frontend-ui-spec.md)）。

## Users

**Primary:** 参与 12 人标准局（预女猎白：4 狼 + 4 民 + 预/女/猎/愚）的真人玩家，在浏览器里与 AI 座位同桌。

**Context:**

- **Home / Setup：** 进入牌室前的氛围与板子选择；期望中世纪、魔法、诡异、奇幻气质
- **对局中：** 注意力在 **左右座位、中央法官/公屏、阶段、倒计时、发言轮次**
- **等待房：** 创建后于 Game 页等待；准备、房主开局
- **夜晚：** 技能操作在底栏；中央为阶段与死讯宣读
- **白天：** 中央公频发言；轮到谁，谁座位高亮
- **结算：** 胜负与身份揭示

**Secondary:** 课题演示与联调（开发者、评委）需要可读阶段推进与事件日志。

## Product Purpose

**werewolf-engine Web UI** 是服务端权威狼人杀引擎的 **对局控制台**：把 `PHASE_SYNC`、倒计时、座位与操作约束转成清晰、可操作的界面。

成功标准：

1. 用户 **3 秒内** 能回答：现在什么阶段、还剩多少秒、我能做什么、**谁在发言**
2. 信息展示遵守 **角色隔离**（狼聊、验人结果等不对错误座位泄露）
3. **Home** 有烛火法庭式世界感；**Game** 像左右列座位的牌室，不像 SaaS 后台
4. 动效服务 **阶段切换、发言提示、倒计时**，不抢戏

**不在 MVP 范围：** 观战大屏、账号体系、加入他人房间 UI、社交分享页。

## Brand Personality

**三个词：** 诡秘、仪式、烛火

**语气：** 简短、像法官与门童播报；不用营销腔，不用感叹号堆叠。

**情感目标:**

- Home：推开石室门，薄雾与烛光，期待与不安
- 夜晚：压抑、专注、信息稀缺
- 白天：紧张、可辩论、公开对抗
- 等待/结算：克制、清晰、可复盘

**参考气质（非像素级抄袭）:**

- 中世纪奇幻牌室：石墙、蜡烛、木桌（Home / brand）
- 12 人 **左右列座位 + 中央法官区**（Game / product）
- 桌游的信息密度与轮次可读性

## Anti-references

**整页/结构:**

- Hero + 三特性卡片 + CTA 的 SaaS landing 骨架
- 左侧固定导航 + 右侧空白内容区的 admin 模板
- 「Welcome to AI 狼人杀」式大标题
- 对称三列 feature grid

**视觉 AI 味（必须避免）:**

- 紫/粉/蓝渐变背景或 gradient text
- 玻璃拟态 `backdrop-blur` 作为默认卡片
- 左侧色条 accent card
- emoji 替代图标系统
- 全屏粒子循环背景

**交互/内容:**

- Modal 作为首选（优先 inline）
- 展示 `PHASE_SYNC` raw JSON
- 用 em dash 写 UI 文案
- 虚构 testimonial、无来源统计

**品类 reflex（一猜就中 = 失败）:**

- 霓虹赛博 + 血红十字
- AI 产品紫渐变 + 机器人插画
- 狼人杀手游卡通 UI 直接照搬

**允许（brand 区）:** 衬线 display、羊皮纸/石纹纹理、低对比奇幻 silhouette、烛火微动

## Design Principles

1. **阶段驱动中间区：** 夜晚中央是法官宣读与死讯；白天中央是公频聊天；换阶段切换主区，不只改顶栏一行字。

2. **左右列座位：** 12 人左 #1–#6、右 #7–#12；中央是信息主舞台，底栏是操作。

3. **发言轮次可见：** `currentSpeakerId` 对应座位高亮 + 文案「发言中」；仅当前发言人可输入公频/遗言。

4. **信息隔离可见：** 狼频道 Tab 与公频分离；用户能感知谁能看到什么。

5. **法官，不是推销员：** 文案像规则播报（「第 2 夜 · 狼人行动」），不像产品宣传。

6. **动效有因果：** 阶段切换、发言高亮、倒计时紧迫；遵守 `prefers-reduced-motion`。

## Accessibility & Inclusion

- 目标 **WCAG 2.1 AA**
- 座位状态 **不仅依赖颜色**（发言中、死亡、选中需文案/描边）
- **`prefers-reduced-motion`**：关闭烛火/发言脉动，保留倒计时数字
- 中文 UI；座位号与倒计时 **tabular nums**
- 触控目标最小 **44×44px**（P1 移动端）

## Technical Context (for agents)

| 项 | 值 |
|----|-----|
| UI 代码 | `frontend/`（React 18 + Vite + TypeScript + Tailwind 3） |
| UI 规格 | [docs/reference/frontend-ui-spec.md](docs/reference/frontend-ui-spec.md) |
| 视觉 token | [DESIGN.md](DESIGN.md) |
| 实时 | WebSocket：`PHASE_SYNC`, `ACTION_ACK`, `CHAT_BROADCAST`, `GAME_EVENT`, `GAME_OVER` |
| 需求基线 | [docs/progress/requirements-mvp-v0.1.md](docs/progress/requirements-mvp-v0.1.md) §4.8 |
| 前端缺口 | [docs/progress/status.md](docs/progress/status.md) §前端 F-01～F-12 |
| 设计 skill | Impeccable；Home=brand，Game=product |

**集成待办落点:**

- F-01/F-02：公频/狼频道 → CenterStage 白天区 Tab
- F-03/F-04：GAME_EVENT → NightAnnouncer 中间区
- F-05：GAME_OVER → 结算 overlay
- F-07～F-12：三屏路由、6+6 布局、发言高亮、boardType

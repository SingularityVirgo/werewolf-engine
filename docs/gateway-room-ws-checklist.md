# gateway / room / WS 联调清单

| 属性 | 值 |
|------|-----|
| 版本 | v0.1 |
| 日期 | 2026-05-18 |
| 适用范围 | B 侧联调层：`gateway` / `room` / WebSocket / Redis 映射 |
| 关联文档 | [requirements-mvp-v0.1.md](requirements-mvp-v0.1.md)、[architecture-design-spec.md](architecture-design-spec.md)、[developer-local-setup.md](developer-local-setup.md) |

---

## 1. 当前进展

从现有 `docs` 和代码看，当前项目状态可以概括为：

| 模块 | 状态 | 说明 |
|------|------|------|
| `game` 状态机 | **基本完成** | 夜晚/白天流转、胜负裁决、`PHASE_SYNC` 构造都已具备 |
| `ai` 骨架 | **基本完成** | `AIService`、`MockAIPlayer`、Prompt/Parse/Guard 分层已存在 |
| `gateway` 正式层 | **未开始** | 还没有正式 `WebSocketHandler`、连接表、路由层 |
| `room` 正式层 | **未开始** | 还没有完整的 HTTP 建房 / ready / start / 解散 API |
| 临时联调用桥 | **已存在** | `InternalGameController` 作为 Week1 临时 HTTP bridge |
| Redis 会话映射 | **未开始** | 还没形成 `roomId + playerId -> sessionId` 的正式闭环 |
| Bot 直连联调 | **未完成** | 需要等正式 WS 协议和网关路由落地 |

---

## 2. 联调层目标

联调层的作用不是替代正式实现，而是先把这条链路跑通：

```text
Client / Bot
  -> gateway
  -> room
  -> game
  -> phase / action result
  -> gateway
  -> Client / Bot
```

它需要保证：

1. 客户端能连上。
2. 房间能创建、加入、准备、开始。
3. `GAME_ACTION` 能进状态机。
4. `PHASE_SYNC` / `ACTION_ACK` 能按座位发回去。
5. 断线、重连、定向推送的基础结构先成立。

---

## 3. 实现清单

### 3.1 `gateway`

| ID | 项 | 说明 | 状态 |
|----|----|------|------|
| G-01 | 原生 `WebSocketHandler` | 接收 `CONNECTED` / `JOIN_ROOM` / `READY` / `GAME_ACTION` | 未开始 |
| G-02 | `WebSocketConfigurer` 注册 | 暴露 `/ws/game` 或约定路径 | 未开始 |
| G-03 | 连接管理器 | 维护 `sessionId`、`roomId`、`playerId`、心跳状态 | 未开始 |
| G-04 | 消息路由器 | 按 `type` 分发到 room / game / chat 逻辑 | 未开始 |
| G-05 | 定向推送 | 按 `roomId` / `playerId` 发送 `PHASE_SYNC`、`ACTION_ACK`、`GAME_EVENT` | 未开始 |
| G-06 | 鉴权接入 | 读取 token 并绑定 user / seat | 未开始 |
| G-07 | 断线重连 | 30s 窗口内恢复同一座位连接 | 未开始 |

### 3.2 `room`

| ID | 项 | 说明 | 状态 |
|----|----|------|------|
| R-01 | 创建房间 | `POST /api/room`，返回 `roomId` | 未开始 |
| R-02 | 加入房间 | 分配 `playerId`，支持 AI 预占位 | 未开始 |
| R-03 | 准备 / 取消准备 | 更新座位 ready 状态 | 未开始 |
| R-04 | 开始游戏 | 校验人数与 ready，触发 `ROLE_ASSIGN` | 未开始 |
| R-05 | 解散房间 | 房主、未开局时可解散 | 未开始 |
| R-06 | 房间快照 | 查询当前座位、状态、阶段 | 未开始 |

### 3.3 Redis

| ID | 项 | 说明 | 状态 |
|----|----|------|------|
| D-01 | 会话映射 | `werewolf:ws:conn:{roomId}:{playerId}` | 未开始 |
| D-02 | 房间玩家集合 | `werewolf:room:{roomId}:players` | 未开始 |
| D-03 | 存活座位集合 | `werewolf:room:{roomId}:alive` | 未开始 |
| D-04 | 当前阶段缓存 | `werewolf:game:{roomId}:phase` | 未开始 |
| D-05 | 状态快照缓存 | `werewolf:game:{roomId}:state` | 未开始 |
| D-06 | 狼队门闩 | `werewolf:game:{roomId}:wolf_chat_in_phase` | 未开始 |

### 3.4 消息闭环

| ID | 项 | 说明 | 状态 |
|----|----|------|------|
| M-01 | `CONNECTED` | 连接成功确认 | 未开始 |
| M-02 | `JOIN_ROOM` | 绑定房间与座位 | 未开始 |
| M-03 | `PHASE_SYNC` | 阶段同步，按座位裁剪字段 | 已有内核，未接网关 |
| M-04 | `ACTION_ACK` | 操作成功 / 拒绝反馈 | 已有内核，未接网关 |
| M-05 | `GAME_EVENT` | 死亡、放逐、公开事件广播 | 未开始 |
| M-06 | `ERROR` | 协议错误与非法操作返回 | 部分存在 |

---

## 4. 推荐落地顺序

1. 先做 `gateway` 的原生 WS 接入和消息分发。
2. 再把 `room` 的 HTTP 建房 / start 补齐。
3. 接入 Redis 会话映射，补重连窗口。
4. 把 `game` 的 `PHASE_SYNC` / `ACTION_ACK` 接到 WS 出口。
5. 最后补 Bot 联调和定向推送压测。

---

## 5. 联调验收

### 5.1 最小可跑通

- [ ] 浏览器或 Bot 能建立 WS 连接
- [ ] 连接后收到 `CONNECTED`
- [ ] `JOIN_ROOM` 后收到正确 `playerId`
- [ ] 创建房间后可看到 `roomId`
- [ ] `start` 后能收到首个 `PHASE_SYNC`
- [ ] 发出 `GAME_ACTION` 能收到 `ACTION_ACK`

### 5.2 阶段同步

- [ ] `PHASE_SYNC.currentPhase` 与服务端一致
- [ ] 角色专属字段只发给正确座位
- [ ] 死亡座位仍能收到允许的公开同步
- [ ] 同房间不同座位不串消息

### 5.3 稳定性

- [ ] 断线后在重连窗口内可恢复
- [ ] 同一房间 mutating 事件串行处理
- [ ] 房间结束后能清理会话与 Redis 映射

---

## 6. 现有临时方案

当前代码里已有一个临时 HTTP 联调入口：

- `src/main/java/com/werewolfengine/game/api/InternalGameController.java`

它适合 Week1 先验证：

1. 状态机能不能跑。
2. `PHASE_SYNC` 的字段是否正确。
3. `ACTION_ACK` 是否能返回。

但它不能替代正式 gateway，因为它还不负责：

- WS 会话生命周期
- 定向推送
- 断线重连
- 房间连接映射

---

## 7. 备注

- 本清单是 B 侧工程化执行页，不是协议冻结页。
- 若后续 `gateway` 接入时影响消息格式，应回写到 PRD / 架构文档。
- 若需要，我可以继续把这个清单拆成更细的 `TODO` 版，直接按文件和类名列出来。


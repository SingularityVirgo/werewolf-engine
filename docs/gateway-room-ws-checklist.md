# gateway / room / WS 联调清单

| 属性 | 值 |
|------|-----|
| 版本 | v0.1 |
| 日期 | 2026-05-18 |
| 适用范围 | B 侧联调层：`gateway` / `room` / WebSocket / Redis 映射 |
| 关联文档 | [requirements-mvp-v0.1.md](progress/requirements-mvp-v0.1.md)、[gateway-room-modules](reference/gateway-room-modules.md)、[ADR-005](adr/005-gateway-push-and-phase-timer.md)、[developer-local-setup.md](developer-local-setup.md) |

> **定位**：本页为 B 侧**执行勾选清单**；架构决策以 [ADR-005](adr/005-gateway-push-and-phase-timer.md) 与 [gateway-room-modules](reference/gateway-room-modules.md) 为准。

---

## 1. 当前进展

从现有 `docs` 和代码看，当前项目状态可以概括为：

| 模块 | 状态 | 说明 |
|------|------|------|
| `game` 状态机 | **基本完成** | 夜晚/白天流转、胜负裁决、`PHASE_SYNC` 构造都已具备 |
| `ai` 骨架 | **基本完成** | `AIService`、`MockAIPlayer`、Prompt/Parse/Guard 分层已存在 |
| `gateway` 正式层 | **部分完成** | 已有 `/ws/game`、`GameWebSocketHandler`、连接表、消息路由；定向推送、重连、鉴权待补 |
| `room` 正式层 | **部分完成** | 已有 HTTP 建房 / join / ready / start / snapshot；解散、房主、完整席位分配待补 |
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
| G-01 | 原生 `WebSocketHandler` | 接收 `CONNECTED` / `JOIN_ROOM` / `READY` / `GAME_ACTION` | 已完成 |
| G-02 | `WebSocketConfigurer` 注册 | 暴露 `/ws/game` 或约定路径 | 已完成 |
| G-03 | 连接管理器 | 维护 `sessionId`、`roomId`、`playerId`、心跳状态 | 部分完成：已有内存映射，心跳待补 |
| G-04 | 消息路由器 | 按 `type` 分发到 room / game / chat 逻辑 | 部分完成：已接 game/phase，chat 待补 |
| G-05 | 定向推送 | 按 `roomId` / `playerId` 发送 `PHASE_SYNC`、`ACTION_ACK`、`GAME_EVENT` | 部分完成：响应已带接收者 seat，主动广播待补 |
| G-06 | 鉴权接入 | 读取 token 并绑定 user / seat | 未开始 |
| G-07 | 断线重连 | 30s 窗口内恢复同一座位连接 | 未开始 |

### 3.2 `room`

| ID | 项 | 说明 | 状态 |
|----|----|------|------|
| R-01 | 创建房间 | `POST /api/room`，返回 `roomId` | 已完成 |
| R-02 | 加入房间 | 分配 `playerId`，支持 AI 预占位 | 部分完成：支持指定 seat 加入，自动分配待补 |
| R-03 | 准备 / 取消准备 | 更新座位 ready 状态 | 已完成 |
| R-04 | 开始游戏 | 校验人数与 ready，触发 `ROLE_ASSIGN` | 已完成 |
| R-05 | 解散房间 | 房主、未开局时可解散 | 未开始 |
| R-06 | 房间快照 | 查询当前座位、状态、阶段 | 部分完成：已有 room/status/phase/round，座位详情待补 |

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
| M-01 | `CONNECTED` | 连接成功确认 | 已完成 |
| M-02 | `JOIN_ROOM` | 绑定房间与座位 | 已完成 |
| M-03 | `PHASE_SYNC` | 阶段同步，按座位裁剪字段 | 已接网关请求-响应，payload 带 `seatId` |
| M-04 | `ACTION_ACK` | 操作成功 / 拒绝反馈 | 已接网关请求-响应，payload 带 `actorSeatId` / `actorPhaseSync` |
| M-05 | `GAME_EVENT` | 死亡、放逐、公开事件广播 | 未开始 |
| M-06 | `ERROR` | 协议错误与非法操作返回 | 部分完成：WS 业务异常已尽量转为 `ERROR`，不直接断线 |

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

- [x] 浏览器或 Bot 能建立 WS 连接
- [x] 连接后收到 `CONNECTED`
- [x] `JOIN_ROOM` 后收到正确 `playerId`
- [x] 创建房间后可看到 `roomId`
- [x] `start` 后能收到首个 `PHASE_SYNC`
- [x] 发出 `GAME_ACTION` 能收到 `ACTION_ACK`

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

## 7. 本地联调记录

### 7.1 启动后端

本地验证时，`8080` 已被占用，因此使用 `8081`：

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

启动成功标志：

```text
Tomcat started on port 8081 (http) with context path '/'
Started WerewolfEngineApplication
```

注意：不要在 `src/main/java/...` 目录里用 `javac WerewolfEngineApplication.java` 单文件启动。Spring Boot 需要 Maven classpath，否则会找不到 `SpringApplication` / `SpringBootApplication`。

### 7.2 HTTP 房间流程

创建房间：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8081/api/room" `
  -ContentType "application/json" `
  -Body '{"roomId":"r_test"}'
```

加入并准备 12 个座位：

```powershell
1..12 | ForEach-Object {
  $seat = $_
  $userId = 1000 + $seat

  Invoke-RestMethod `
    -Method Post `
    -Uri "http://127.0.0.1:8081/api/room/r_test/join" `
    -ContentType "application/json" `
    -Body "{`"seatId`":$seat,`"userId`":$userId}"
}
```

开始游戏：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8081/api/room/r_test/start"
```

已验证返回 `success = true`，首个阶段进入 `NIGHT_WOLF`，并返回 `phaseSyncs`。

### 7.3 浏览器 WebSocket 验证

先打开一个本地 HTTP 页面，避免 Firefox / 浏览器隐私策略拦截本地 `ws://`：

```text
http://127.0.0.1:8081/api/room/r_test
```

然后在浏览器 Console 执行：

```javascript
let ws = new WebSocket("ws://127.0.0.1:8081/ws/game");

ws.onopen = () => console.log("WS 已连接");
ws.onmessage = e => console.log("收到:", JSON.parse(e.data));
ws.onerror = e => console.log("WS 错误:", e);
ws.onclose = e => console.log("WS 关闭:", e.code, e.reason);
```

已验证连接成功后会收到：

```text
WS 已连接
收到: { type: "CONNECTED", payload: ... }
```

绑定已存在座位：

```javascript
ws.send(JSON.stringify({
  type: "JOIN_ROOM",
  payload: {
    roomId: "r_test",
    seatId: 1
  }
}));
```

请求座位视角阶段同步：

```javascript
ws.send(JSON.stringify({
  type: "PHASE_SYNC",
  payload: {
    roomId: "r_test",
    seatId: 1
  }
}));
```

提交动作：

```javascript
ws.send(JSON.stringify({
  type: "GAME_ACTION",
  payload: {
    roomId: "r_test",
    playerId: 1,
    action: "KILL",
    target: 2,
    phase: "NIGHT_WOLF"
  }
}));
```

已验证 `CONNECTED`、`JOIN_ROOM`、`PHASE_SYNC`、`ACTION_ACK` 都可以经 WS 返回。

### 7.4 已知联调注意点

- 当前房间开局后，`JOIN_ROOM` 不要再带 `userId`；只带 `roomId + seatId` 表示把 WS 绑定到已有座位。
- 开局前 `JOIN_ROOM + userId` 可以用于加入座位；开局后再调用真实 join 会被 `RoomService` 拒绝。
- 之前出现过 WS `1011`，原因是业务异常冒泡导致服务端关闭连接；现已调整为尽量返回 `ERROR` 消息。
- `PHASE_SYNC` 请求-响应已由网关在 payload 外层补充 `seatId`。
- `ACTION_ACK` 已由网关补充 `actorSeatId` 和操作者自己的 `actorPhaseSync`，前端/Bot 可以直接刷新当前操作者视角。
- `ActionResult.phaseSyncs` 当前仍然没有携带接收者 `seatId`，所以主动定向推送不能直接依赖该列表，否则有私密视角发错人的风险。后续要做主动推送，建议先补“接收者 seatId”或封装 `TargetedPhaseSync`。

---

## 8. 后续任务

| ID | 项 | 说明 | 优先级 |
|----|----|------|--------|
| N-01 | WS 错误稳定性 | 异常统一返回 `ERROR`，避免 1011 断线 | P0 |
| N-02 | 定向同步协议 | 为 `PHASE_SYNC` 补接收者 seatId / targeted wrapper | P0，部分完成：请求-响应外层已有 seatId |
| N-03 | 主动推送 | `GAME_ACTION` 后按座位推送最新 `PHASE_SYNC` | P1 |
| N-04 | 自动分配座位 | `join` 不传 seatId 时自动找空座 | P1 |
| N-05 | 断线重连 | 30s 内恢复 `roomId + seatId` 绑定 | P1 |
| N-06 | Redis 映射 | 把内存连接表迁移/同步到 Redis key | P2 |
| N-07 | 鉴权绑定 | token -> userId -> seat 权限校验 | P2 |

---

## 9. 备注

- 本清单是 B 侧工程化执行页，不是协议冻结页。
- 若后续 `gateway` 接入时影响消息格式，应回写到 PRD / 架构文档。
- 若需要，我可以继续把这个清单拆成更细的 `TODO` 版，直接按文件和类名列出来。

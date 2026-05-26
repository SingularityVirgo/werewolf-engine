# Bot 与压测设计（C 侧）

| 属性 | 值 |
|------|-----|
| 版本 | v0.1 |
| 日期 | 2026-05-18 |
| 代码 | [scripts/](../../scripts/) |
| 协议 | [PRD §4.6、§6](../progress/requirements-mvp-v0.1.md) |
| B 字段对照 | [gateway-room-modules](gateway-room-modules.md) |

---

## 1. 双路径（必读）

| 路径 | 入口 | 用途 | Week1 验收 |
|------|------|------|------------|
| **A — Internal** | `POST /internal/game/...` | A 测 SM、`mock-auto-play`、tick 压测 | ✅ 可跑通整局 |
| **B — Formal** | `POST /api/room` + `WS /ws/game` | 产品联调、Day4 五项 | ✅ B 推送/tick 已实现；跑 [run_day4_formal.py](../../scripts/formal/run_day4_formal.py) |

**压测报告 v0.1** 须写明使用的路径；仅 A 不能证明 Formal 协议闭环。

---

## 2. Formal 路径：字段对照（C ↔ B）

| 步骤 | C 当前（需改） | B 当前 / 目标 |
|------|----------------|---------------|
| 默认端口 | ~~`8090`~~ → **`8080`** | `8080`（`application.properties`） |
| HTTP 建房 | `{ "aiCount": 11 }` | `{ "roomId"? }`（`aiCount` 待实现） |
| HTTP join | 无 body + Bearer | `{ "seatId", "userId" }` |
| join 响应 | 期望 `playerId` | 返回 `seatId`（= playerId） |
| WS URL | `?token={userId}` | 接受但未解析 token |
| `CONNECTED` | 读 `playerId` | 返回 `sessionId` |
| WS `JOIN_ROOM` | 仅 `roomId` | 必填 `roomId` + `seatId`；可选 `userId` |
| WS `READY` | 仅 `ready` | 必填 `roomId` + `seatId` |
| WS `GAME_ACTION` | 缺字段 | 必填 `roomId`、`playerId`、`action`、`phase` |
| `PHASE_SYNC` | 扁平 payload | **Targeted**：`{ seatId, phaseSync }` |

---

## 3. Day4 联调脚本（目标）

运行 `scripts/formal/run_day4_formal.py`（已实现）：

1. 房主：`POST /api/room` → `roomId`
2. 12 座：`POST .../join` + 各座 WS `JOIN_ROOM`
3. 全员 `ready` → 房主 `start`
4. 断言首包阶段 ∈ `{ ROLE_ASSIGN, NIGHT_WOLF }`
5. 狼座 `GAME_ACTION` `WOLF_CHAT` → `ACTION_ACK`
6. 两房间并行，断言 `roomId` / `phaseSync` 不交叉

启动间隔：建议 0.3s/ Bot（README 已有说明）。

---

## 4. 已有脚本

| 文件 | 路径 | 说明 |
|------|------|------|
| `auto_play_client.py` | `scripts/internal/` | 一键 `mock-auto-play` |
| `tick_play_client.py` | `scripts/internal/` | 循环 `phase-tick` |
| `export_action_log.py` | `scripts/internal/` | 导出并校验 log |
| `run_day4_formal.py` | `scripts/formal/` | Day4 五项 + 可选双房隔离 |
| `formal_path_smoke.py` | `scripts/formal/` | Formal 全链路冒烟 |
| `countdown_observe.py` | `scripts/formal/` | P-05 countdown 验收 |
| `formal_llm_smoke.py` | `scripts/formal/` | Formal + LLM 整局 |
| `lib/bot_player.py` | `scripts/lib/` | 高层 API（Formal 对齐） |

环境变量：

```bash
export WEREWOLF_BASE_URL=http://127.0.0.1:8080
export WEREWOLF_WS_URL=ws://127.0.0.1:8080
```

---

## 5. 压测报告 v0.1 模板

```markdown
# 压测报告 v0.1

- 日期：
- 路径：A（internal） / B（formal WS）
- 局数：
- 成功局数 / 成功率：
- 平均局时长：
- 失败样例：roomId、requestId、最后 phase、日志摘录
- 非法操作拒绝：是 / 否
- 备注：
```

### Week1 最低交付（C）

- [x] Formal Day4 脚本可重复运行（`run_day4_formal.py`）
- [ ] 报告 v0.1（≥20 局 internal 或 ≥5 局 formal，写明路径）

### Week2（PRD §8.3）

- [ ] 100 局、成功率 > 95%
- [ ] 5 个非法操作用例（预期 `ACTION_ACK` 失败或 `ERROR`）

---

## 6. Bot 职责边界

- **实现**：协议客户端、并发、日志、压测统计。
- **不实现**：规则、胜负、技能合法性（均属 `game`）。

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-05-18 | 初稿 |

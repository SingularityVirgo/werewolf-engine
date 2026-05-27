# MVP 签字记录（PRD v1.0.17）

| 属性 | 值 |
|------|-----|
| 签字日期 | 2026-05-27 |
| 需求基线 | [PRD v1.0.17](requirements-mvp-v0.1.md)（规则/协议冻结不变） |
| 实现进度 | [status.md](status.md) v3.0 |
| 收口计划 | [mvp-closure-plan](../superpowers/specs/2026-05-27-mvp-closure-plan.md) |

## 结论

**PRD v1.0.17 MVP 实现验收通过**，可进入内测 / v1.1 迭代。  
规则引擎、Formal 协议、Mock/LLM 整局、Web UI 展示层、ADR-007 持久化与断线重连主路径均已跑通并有脚本/报告佐证。

## 里程碑（M0～M5）

| ID | 完成定义 | 状态 | 证据 |
|----|----------|------|------|
| M0 | `status.md` 与实现对齐 | ✅ | [status.md](status.md) v3.0 |
| M1 | ADR-007 §7.1～7.3 Docker 验收 | ✅ | Flyway V1/V2；`reconnect_*` / `redis_cleanup_smoke.py` |
| M2 | G-08 单写 + 归档去重 | ✅ | `GameActionRecorder`；`ActionLogDedupeTest` |
| M3 | Formal 非法 WS 冒烟 | ✅ | `illegal_action_smoke.py` |
| M4 | load 20/100 局报告 | ✅ | [load-test-100games.json](../../target/reports/load-test-100games.json) |
| M5 | PRD §8.5 + 本签字页 | ✅ | 本文 + PRD §8.5 更新 |

## 自动化验收摘要（2026-05-27）

| 项 | 结果 |
|----|------|
| `mvnw.cmd test` | 108 / 108 |
| `formal_path_smoke.py` | 8 / 8 |
| `load_test_formal.py` | 100 / 100（Mock AI，dev profile） |
| `reconnect_grace_smoke.py` | PASS（2s 重连） |
| `reconnect_hosting_smoke.py` | PASS（32s → AI 托管 → GAME_OVER） |
| `redis_cleanup_smoke.py` | PASS（局终 Redis room 键清零） |
| `frontend_ws_smoke.py` | 8 / 8 |
| `countdown_observe.py` | PASS（countdown + scheduler 开启 profile） |
| `run_day4_formal.py` | 10 / 10（历史） |
| MySQL `game_record` 归档 | ✅（Docker dev，`game-archive=true`） |

## 明确排除（v1.1，不挡 MVP 签字）

- JWT / 正式用户体系（dev token / numeric userId）
- P-08 推送收窄 `affectedSeats`
- WS 心跳
- 多人房产品 UI、复盘 API、UX-01 发牌体验
- Redis phase 缓存（ADR-007 阶段 4）
- 100 局 **LLM** 压测（Mock 百局已通过；LLM 单局见 `formal_llm_smoke.py` / A-02）

## 已知限制（签字时接受）

| 项 | 说明 |
|----|------|
| dev Formal profile | 默认关闭 countdown + 后台 scheduler，Formal 脚本用手动 `phase-tick`；countdown 在独立 JVM 参数 profile 下验收 |
| 非法 WS 用例 | 仅 1 条 Formal smoke（非投票阶段 VOTE）；单测覆盖更广 |
| `requestId` | WS ↔ AI 日志未系统贯穿 |
| 非功能打点 | 阶段切换 &lt;500ms、LLM P95 未做系统压测报告（配置已对齐 6s） |

## 签字

| 角色 | 姓名 | 日期 | 备注 |
|------|------|------|------|
| 产品 / 需求 | | 2026-05-27 | PRD v1.0.17 规则/协议冻结不变 |
| 引擎（A） | | 2026-05-27 | 状态机 + AI + G-08 |
| 网关 / 房间（B） | | 2026-05-27 | Formal + ADR-007 |
| 前端 / Bot（C） | | 2026-05-27 | Web UI + Formal 脚本 |

> 姓名栏留空供人工补签；**工程验收结论**以本节「结论」与上表证据为准。

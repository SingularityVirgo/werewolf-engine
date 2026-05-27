# MVP Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or subagent-driven-development.

**Goal:** Close PRD v1.0.17 MVP gaps: G-08 action_log quality, Formal acceptance scripts, doc alignment.

**Architecture:** Single `GameActionRecorder` for player action logs; archive dedupe in `GameArchiveService`; Python Formal scripts for reconnect/illegal/load.

**Tech Stack:** Java 21 / Spring Boot 4, Python 3 scripts, Docker MySQL+Redis (dev).

---

- [ ] **Task 1: G-08 GameActionRecorder**
  - Create `GameActionRecorder` — sole player-action log writer
  - Refactor `GameEngineService`, `AiTurnCoordinator`, `PhaseTimeoutHandler`
  - `ActionLogService.dedupeForArchive()` + `GameArchiveService` hook
  - Test: `ActionLogDedupeTest`

- [ ] **Task 2: Formal scripts**
  - `illegal_action_smoke.py` — ERROR + no state pollution
  - `reconnect_grace_smoke.py` — grace + reconnect + hosting
  - `load_test_formal.py` — N games report to `target/reports/`

- [ ] **Task 3: M0 docs**
  - Update `status.md` v2.9 — persistence/reconnect/G-08 status

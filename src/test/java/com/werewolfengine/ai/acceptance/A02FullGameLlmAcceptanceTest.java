package com.werewolfengine.ai.acceptance;

import com.werewolfengine.ai.parse.AiIntentParser;
import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.observability.ActionLogEntry;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.orchestration.MockGameRunner;
import com.werewolfengine.game.orchestration.PhaseTimeoutHandler;
import com.werewolfengine.game.orchestration.TurnActorResolver;
import com.werewolfengine.game.sync.PhaseCountdown;
import com.werewolfengine.game.testsupport.GameTestAiSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A-02 live acceptance: 12-seat full game with LLM-first policy, Mock only on fallback.
 * Writes {@code target/reports/a02-full-game-*.json}. Requires {@code DEEPSEEK_API_KEY}.
 */
@SpringBootTest(properties = {
        "werewolf.ai.enabled=true",
        "werewolf.ai.wolves-only=false",
        "werewolf.game.phase-countdown-enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration"
})
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class A02FullGameLlmAcceptanceTest {

  private static final List<String> PARSE_SAMPLES = List.of(
          "{\"thinking\":\"t\",\"action\":\"KILL\",\"target\":3,\"reason\":\"r\"}",
          "{\"action\":\"WOLF_CHAT\",\"content\":\"今晚刀8\",\"reason\":\"商议\"}",
          "{\"action\":\"SKIP\",\"reason\":\"不救\"}",
          "```json\n{\"action\":\"CHECK\",\"target\":5,\"reason\":\"查\"}\n```",
          "Result: {\"action\":\"VOTE\",\"target\":2,\"reason\":\"投\"}",
          "{\"thinking\":\"x\",\"action\":\"SPEAK\",\"content\":\"过\",\"reason\":\"说\"}",
          "{\"action\":\"SKIP_SPEAK\",\"reason\":\"过\"}",
          "{\"action\":\"SKIP_VOTE\",\"reason\":\"弃\"}",
          "{\"action\":\"SAVE\",\"target\":8,\"reason\":\"救\"}",
          "{\"action\":\"POISON\",\"target\":4,\"reason\":\"毒\"}",
          "{\"action\":\"SHOOT\",\"target\":6,\"reason\":\"枪\"}"
  );

  @Autowired
  private GameStateMachine stateMachine;

  @Autowired
  private MockGameRunner mockGameRunner;

  @Autowired
  private ActionLogService actionLog;

  @AfterEach
  void restoreCountdown() {
    PhaseCountdown.setEnabled(true);
  }

  @Test
  void fullGame_llmFirstWithMockFallback_writesAcceptanceReport() throws Exception {
    int parseOk = 0;
    AiIntentParser parser = new AiIntentParser();
    for (int i = 0; i < 50; i++) {
      try {
        parser.parse(PARSE_SAMPLES.get(i % PARSE_SAMPLES.size()));
        parseOk++;
      } catch (RuntimeException ignored) {
        // count failure
      }
    }
    boolean parsePassed = parseOk >= 48;
    boolean g06Passed = verifyG06WolfTimeoutFallback();
    PhaseCountdown.setEnabled(false);

    String roomId = "a02_" + System.nanoTime();
    stateMachine.createRoom(roomId);
    stateMachine.markAllReady(roomId);
    assertThat(stateMachine.startGame(roomId).success()).isTrue();

    MockGameRunner.RunResult run = mockGameRunner.runUntilGameOver(roomId, 12_000);
    List<ActionLogEntry> log = actionLog.getLog(roomId);
    List<GamePhase> phasesSeen = log.stream()
            .map(ActionLogEntry::phase)
            .distinct()
            .toList();
    AiAcceptanceReport.Metrics metrics = AiAcceptanceReport.from(
            run, phasesSeen, log, parsePassed, parseOk, 50, g06Passed);

    var reportPath = AiAcceptanceReport.write("a02-full-game", metrics, Map.of(
            "roomId", roomId,
            "modelId", "deepseek-v4-flash",
            "maxSteps", 12_000,
            "aiPolicy", "llm-first-mock-fallback",
            "wolvesOnly", false
    ));

    System.out.println("\n=== A-02 acceptance report ===");
    System.out.println("file: " + reportPath.toAbsolutePath());
    System.out.println("finished: " + metrics.gameFinished() + " winner=" + metrics.winner());
    System.out.println("steps: " + metrics.schedulerSteps());
    System.out.println("playerActions: " + metrics.playerActions()
            + " llm=" + metrics.llmActions()
            + " mockFallback=" + metrics.mockFallbackActions());
    System.out.println("llm ratio: " + String.format("%.1f%%", metrics.llmActionRatio() * 100));
    System.out.println("parse: " + parseOk + "/50");
    System.out.println("g06 wolf timeout unit: " + g06Passed);
    System.out.println("mockOnly: " + metrics.mockOnlyActions());
    System.out.println("byPhase: " + metrics.actionsByPhase());

    assertThat(g06Passed).as("G-06 wolf phase timeout fallback").isTrue();
    assertThat(parsePassed).as("JSON parse rate >= 95%").isTrue();
    assertThat(run.isFinished()).as("game reaches GAME_OVER").isTrue();
    assertThat(metrics.llmActions()).as("at least one LLM-tagged action").isGreaterThan(0);
    assertThat(metrics.mockOnlyActions()).as("no mock-only when LLM enabled").isZero();
    assertThat(phasesSeen).as("night and day phases observed in log")
            .contains(GamePhase.NIGHT_WOLF, GamePhase.DAY_VOTE);
  }

  private static boolean verifyG06WolfTimeoutFallback() {
    boolean prev = PhaseCountdown.isEnabled();
    PhaseCountdown.setEnabled(true);
    try {
      GameStateMachine sm = new GameStateMachine();
      PhaseTimeoutHandler handler = new PhaseTimeoutHandler(
              sm,
              new TurnActorResolver(),
              GameTestAiSupport.disabledAiService(),
              new MockAIPlayer(),
              null,
              new com.werewolfengine.game.observability.GameActionRecorder(sm, null)
      );
      String roomId = "g06_accept";
      sm.createRoom(roomId);
      sm.markAllReady(roomId);
      sm.startGame(roomId);
      GameRoomState room = sm.getRoom(roomId).orElseThrow();
      room.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1);
      if (!handler.applyIfExpired(roomId, room)) {
        return false;
      }
      GameRoomState after = sm.getRoom(roomId).orElseThrow();
      if (after.getPhase() != GamePhase.NIGHT_SEER || after.getPendingWolfKillTarget() == null) {
        return false;
      }
      PlayerState victim = after.getPlayer(after.getPendingWolfKillTarget());
      return victim != null && victim.getRole() != Role.WEREWOLF;
    } finally {
      PhaseCountdown.setEnabled(prev);
    }
  }
}

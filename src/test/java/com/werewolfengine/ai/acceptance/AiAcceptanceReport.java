package com.werewolfengine.ai.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.werewolfengine.ai.api.DecisionResult;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameWinner;
import com.werewolfengine.game.observability.ActionLogEntry;
import com.werewolfengine.game.orchestration.MockGameRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A-02 acceptance metrics (PRD §1.2 P1, §8.3) — written to {@code target/reports/}.
 */
public final class AiAcceptanceReport {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private AiAcceptanceReport() {
    }

    public record Metrics(
            boolean gameFinished,
            GameWinner winner,
            int schedulerSteps,
            MockGameRunner.RunResult.Outcome outcome,
            int totalLogEntries,
            int playerActions,
            int llmActions,
            int mockFallbackActions,
            int mockOnlyActions,
            int thinkingLines,
            Map<String, Integer> actionsByPhase,
            Map<String, Integer> actionsByType,
            Map<String, Integer> modelIds,
            List<String> phasesSeen,
            double llmActionRatio,
            boolean parseRatePassed,
            int parseSuccesses,
            int parseAttempts,
            boolean g06WolfTimeoutUnitPassed
    ) {
    }

    public static Metrics from(
            MockGameRunner.RunResult run,
            List<GamePhase> phasesSeen,
            List<ActionLogEntry> log,
            boolean parseRatePassed,
            int parseSuccesses,
            int parseAttempts,
            boolean g06WolfTimeoutUnitPassed
    ) {
        List<ActionLogEntry> actions = log.stream()
                .filter(e -> e.action() != null)
                .toList();

        int llm = 0;
        int mockFb = 0;
        int mockOnly = 0;
        int thinking = 0;
        Map<String, Integer> byPhase = new LinkedHashMap<>();
        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<String, Integer> modelIds = new LinkedHashMap<>();

        for (ActionLogEntry e : log) {
            if (e.thinking() != null && !e.thinking().isBlank()) {
                thinking++;
            }
            String mid = e.modelId();
            if (mid != null) {
                modelIds.merge(mid, 1, Integer::sum);
            }
        }

        for (ActionLogEntry e : actions) {
            byPhase.merge(e.phase().name(), 1, Integer::sum);
            byType.merge(e.action().name(), 1, Integer::sum);
            String mid = e.modelId();
            if (mid == null) {
                continue;
            }
            if (DecisionResult.MOCK_FALLBACK_MODEL.equals(mid)) {
                mockFb++;
            } else if (DecisionResult.MOCK_ONLY_MODEL.equals(mid)) {
                mockOnly++;
            } else {
                llm++;
            }
        }

        int playerActions = actions.size();
        double ratio = playerActions == 0 ? 0.0 : (double) llm / playerActions;

        return new Metrics(
                run.isFinished(),
                run.winner(),
                run.steps(),
                run.outcome(),
                log.size(),
                playerActions,
                llm,
                mockFb,
                mockOnly,
                thinking,
                byPhase,
                byType,
                modelIds,
                phasesSeen.stream().map(GamePhase::name).distinct().toList(),
                ratio,
                parseRatePassed,
                parseSuccesses,
                parseAttempts,
                g06WolfTimeoutUnitPassed
        );
    }

    public static Path write(String suite, Metrics metrics, Map<String, Object> extra) throws Exception {
        Path dir = Path.of("target", "reports");
        Files.createDirectories(dir);
        String ts = Instant.now().toString().replace(":", "-");
        Path out = dir.resolve(suite + "-" + ts + ".json");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("suite", suite);
        doc.put("generatedAt", Instant.now().toString());
        doc.put("policy", "llm-first-mock-fallback");
        doc.put("metrics", metrics);
        if (extra != null) {
            doc.putAll(extra);
        }

        List<Map<String, Object>> checks = buildChecks(metrics);
        doc.put("checks", checks);
        doc.put("passed", checks.stream().allMatch(c -> Boolean.TRUE.equals(c.get("pass"))));

        JSON.writeValue(out.toFile(), doc);
        return out;
    }

    private static List<Map<String, Object>> buildChecks(Metrics m) {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(check("game_over", m.gameFinished(), "outcome=" + m.outcome() + " winner=" + m.winner()));
        checks.add(check("parse_rate_95pct", m.parseRatePassed(),
                m.parseSuccesses() + "/" + m.parseAttempts()));
        checks.add(check("llm_used", m.llmActions() > 0, "llmActions=" + m.llmActions()));
        checks.add(check("multi_phase_llm",
                m.phasesSeen().stream().anyMatch(p -> p.startsWith("NIGHT"))
                        && m.phasesSeen().contains("DAY_VOTE"),
                "phasesSeen=" + m.phasesSeen()));
        checks.add(check("mock_fallback_ratio",
                m.playerActions() == 0 || (double) m.mockFallbackActions() / m.playerActions() < 0.5,
                "mockFallback=" + m.mockFallbackActions() + "/" + m.playerActions()));
        checks.add(check("no_mock_only_when_llm_enabled", m.mockOnlyActions() == 0,
                "mockOnly=" + m.mockOnlyActions()));
        checks.add(check("g06_wolf_timeout_unit", m.g06WolfTimeoutUnitPassed(), "PhaseTimeoutHandler G-06"));
        return checks;
    }

    private static Map<String, Object> check(String name, boolean pass, String detail) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("name", name);
        c.put("pass", pass);
        c.put("detail", detail);
        return c;
    }
}

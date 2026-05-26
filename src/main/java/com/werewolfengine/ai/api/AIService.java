package com.werewolfengine.ai.api;

import com.werewolfengine.ai.agent.AiAgent;
import com.werewolfengine.ai.config.AiProperties;
import com.werewolfengine.ai.guard.AiLegalActions;
import com.werewolfengine.ai.memory.MemoryPromptFormatter;
import com.werewolfengine.ai.parse.AiIntentParser;
import com.werewolfengine.ai.perceive.GameViewContext;
import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.ai.prompt.AiPromptBuilder;
import com.werewolfengine.ai.prompt.Persona;
import com.werewolfengine.game.observability.ActionLogEntry;
import com.werewolfengine.game.view.SeatPerceptionProjector;
import com.werewolfengine.game.view.SeatPerceptionSlice;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.view.GameView;
import com.werewolfengine.game.view.GameViews;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * LangChain4j + DeepSeek intent generation (PRD §4.5). Every AI seat tries LLM first when enabled;
 * {@link MockAIPlayer} is used only when LLM is off, times out, parse fails after retries, or intent is illegal.
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final AiProperties properties;
    private final MockAIPlayer mockAIPlayer;
    private final AiPromptBuilder promptBuilder;
    private final MemoryPromptFormatter memoryFormatter;
    private final AiIntentParser intentParser;
    private final AiLegalActions legalActions;
    private final Optional<ChatModel> chatModel;
    private final Optional<ActionLogService> actionLog;
    private final ExecutorService llmExecutor;
    private final ConcurrentHashMap<String, AiAgent> agents = new ConcurrentHashMap<>();

    public AIService(
            AiProperties properties,
            MockAIPlayer mockAIPlayer,
            AiPromptBuilder promptBuilder,
            MemoryPromptFormatter memoryFormatter,
            AiIntentParser intentParser,
            AiLegalActions legalActions,
            @Autowired(required = false) ChatModel chatModel,
            @Autowired(required = false) ActionLogService actionLog
    ) {
        this.properties = properties;
        this.mockAIPlayer = mockAIPlayer;
        this.promptBuilder = promptBuilder;
        this.memoryFormatter = memoryFormatter;
        this.intentParser = intentParser;
        this.legalActions = legalActions;
        this.chatModel = Optional.ofNullable(chatModel);
        this.actionLog = Optional.ofNullable(actionLog);
        this.llmExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Optional<PlayerIntent> decide(GameRoomState room, int playerId) {
        return decideWithSource(room, playerId).map(DecisionResult::intent);
    }

    /**
     * LLM-first for all acting AI seats when {@link AiProperties#isEnabled()} and not {@link AiProperties#isWolvesOnly()}
     * (or wolf night only in P0.5 mode).
     */
    public Optional<DecisionResult> decideWithSource(GameRoomState room, int playerId) {
        if (isHumanSeat(room, playerId)) {
            return Optional.empty();
        }
        if (legalActions.allowed(room, playerId).isEmpty()) {
            return Optional.empty();
        }

        Optional<PlayerIntent> mockFallback = mockAIPlayer.decide(room, playerId);

        if (!shouldUseLlm(room, playerId)) {
            return mockFallback.map(intent -> new DecisionResult(
                    intent,
                    DecisionResult.Source.MOCK_ONLY,
                    DecisionResult.MOCK_ONLY_MODEL
            ));
        }

        try {
            PlayerIntent fromLlm = requestLlmIntentWithRetries(room, playerId);
            if (legalActions.isLegal(room, playerId, fromLlm)) {
                log.debug("LLM intent room={} seat={} action={}", room.getRoomId(), playerId, fromLlm.action());
                recordLlmThinking(room, playerId, fromLlm);
                return Optional.of(new DecisionResult(
                        fromLlm,
                        DecisionResult.Source.LLM,
                        properties.getModelId()
                ));
            }
            log.warn("LLM illegal intent room={} seat={} action={}, using mock fallback",
                    room.getRoomId(), playerId, fromLlm.action());
        } catch (Exception e) {
            log.warn("LLM failed room={} seat={}: {}, using mock fallback",
                    room.getRoomId(), playerId, e.toString());
        }

        return mockFallback.map(intent -> new DecisionResult(
                intent,
                DecisionResult.Source.MOCK_FALLBACK,
                DecisionResult.MOCK_FALLBACK_MODEL
        ));
    }

    private AiAgent agentFor(GameRoomState room, int playerId) {
        String key = room.getRoomId() + ":" + playerId;
        return agents.computeIfAbsent(key, k -> new AiAgent(playerId));
    }

    private static boolean isHumanSeat(GameRoomState room, int playerId) {
        PlayerState p = room.getPlayer(playerId);
        return p != null && p.getHumanUserId() != null;
    }

    private boolean shouldUseLlm(GameRoomState room, int playerId) {
        if (isHumanSeat(room, playerId)) {
            return false;
        }
        if (!properties.isEnabled() || chatModel.isEmpty()) {
            return false;
        }
        if (!properties.isWolvesOnly()) {
            return true;
        }
        PlayerState p = room.getPlayer(playerId);
        Role role = p != null ? p.getRole() : null;
        return room.getPhase() == GamePhase.NIGHT_WOLF && role == Role.WEREWOLF;
    }

    private PlayerIntent requestLlmIntentWithRetries(GameRoomState room, int playerId) {
        int maxAttempts = properties.getMaxLlmRetries() + 1;
        IllegalArgumentException lastParseError = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return requestLlmIntent(room, playerId);
            } catch (IllegalArgumentException e) {
                lastParseError = e;
                if (attempt == maxAttempts - 1) {
                    throw e;
                }
                log.warn("LLM parse failed for room={} seat={} (attempt {}/{}): {}",
                        room.getRoomId(), playerId, attempt + 1, maxAttempts, e.getMessage());
            }
        }
        throw lastParseError != null ? lastParseError : new IllegalStateException("LLM parse failed");
    }

    private PlayerIntent requestLlmIntent(GameRoomState room, int playerId) {
        Future<PlayerIntent> future = llmExecutor.submit(() -> callLlm(room, playerId));
        try {
            return future.get(properties.getLlmTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("LLM timeout after " + properties.getLlmTimeoutSeconds() + "s", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("LLM call failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM call interrupted", e);
        }
    }

    private PlayerIntent callLlm(GameRoomState room, int playerId) {
        GameView gameView = GameViews.forSeat(room, playerId);
        GameViewContext view = GameViewContext.from(gameView);
        Set<GameActionType> allowed = legalActions.allowed(room, playerId);
        if (allowed.isEmpty()) {
            throw new IllegalStateException("no legal actions for seat " + playerId);
        }
        Persona persona = agentFor(room, playerId).persona();
        String system = promptBuilder.systemMessage(persona);
        String memoryBlock = buildMemoryBlock(room, playerId);
        String user = promptBuilder.userMessage(view, allowed, memoryBlock);
        ChatResponse response = chatModel.orElseThrow().chat(
                List.of(SystemMessage.from(system), UserMessage.from(user))
        );
        return intentParser.parse(llmPayload(response.aiMessage()));
    }

    private String buildMemoryBlock(GameRoomState room, int playerId) {
        if (!properties.getMemory().isEnabled() || actionLog.isEmpty()) {
            return null;
        }
        List<ActionLogEntry> log = actionLog.get().getLog(room.getRoomId());
        SeatPerceptionSlice slice = SeatPerceptionProjector.project(room, playerId, log);
        var opts = properties.getMemory();
        var formatOpts = new MemoryPromptFormatter.FormatOptions(
                opts.getMaxEvents(),
                opts.getMaxChars(),
                opts.isIncludeOwnThinking()
        );
        return memoryFormatter.format(slice, formatOpts, true).text();
    }

    private void recordLlmThinking(GameRoomState room, int playerId, PlayerIntent fromLlm) {
        if (fromLlm.thinking() == null || fromLlm.thinking().isBlank()) {
            return;
        }
        actionLog.ifPresent(log -> {
            var p = room.getPlayer(playerId);
            log.recordAiThinking(
                    room.getRoomId(),
                    playerId,
                    p != null ? p.getRole() : null,
                    room.getPhase(),
                    room.getRound(),
                    fromLlm.action(),
                    fromLlm.thinking(),
                    properties.getModelId()
            );
        });
    }

    static String llmPayload(AiMessage message) {
        String text = message.text();
        if (text != null && !text.isBlank()) {
            return text.trim();
        }
        String thinking = message.thinking();
        if (thinking != null && !thinking.isBlank()) {
            return thinking.trim();
        }
        throw new IllegalArgumentException("empty LLM response");
    }
}

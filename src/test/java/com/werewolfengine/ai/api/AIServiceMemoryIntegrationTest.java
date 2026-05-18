package com.werewolfengine.ai.api;

import com.werewolfengine.ai.config.AiProperties;
import com.werewolfengine.ai.guard.AiLegalActions;
import com.werewolfengine.ai.memory.MemoryPromptFormatter;
import com.werewolfengine.ai.parse.AiIntentParser;
import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.ai.prompt.AiPromptBuilder;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.observability.ActionLogEntry;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.orchestration.AiTurnCoordinator;
import com.werewolfengine.game.orchestration.MockGameRunner;
import com.werewolfengine.game.orchestration.TurnActorResolver;
import com.werewolfengine.game.view.PerceptionEventKind;
import com.werewolfengine.game.view.SeatPerceptionProjector;
import com.werewolfengine.game.view.SeatPerceptionSlice;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ADR-004 §9 — Memory wired into {@link AIService} LLM user prompt (M3b–M3d).
 */
class AIServiceMemoryIntegrationTest {

    private ActionLogService actionLog;
    private GameStateMachine stateMachine;
    private AiTurnCoordinator coordinator;
    private ChatModel chatModel;
    private AIService aiService;

    @BeforeEach
    void setUp() {
        actionLog = new ActionLogService();
        stateMachine = new GameStateMachine(actionLog);
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setWolvesOnly(false);
        properties.getMemory().setEnabled(true);
        chatModel = mock(ChatModel.class);
        aiService = new AIService(
                properties,
                new MockAIPlayer(),
                new AiPromptBuilder(),
                new MemoryPromptFormatter(),
                new AiIntentParser(),
                new AiLegalActions(),
                chatModel,
                actionLog
        );
        coordinator = new AiTurnCoordinator(
                stateMachine,
                new TurnActorResolver(),
                aiService,
                actionLog
        );
    }

    @Test
    void m3b_day2DiscussPromptIncludesNightDeathAndVote() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        when(chatModel.chat(captor.capture())).thenReturn(llmSkipSpeakResponse());

        String roomId = "mem_m3b_" + System.nanoTime();
        startRoom(roomId);
        assertThat(advanceUntil(roomId, r -> r.getRound() >= 2 && r.getPhase() == GamePhase.DAY_DISCUSS))
                .isTrue();

        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        int speaker = room.getDiscussOrder().get(room.getDiscussIndex());
        aiService.decide(room, speaker);

        String user = userText(captor.getValue());
        assertThat(user).contains("## 本局记忆");
        boolean hasNightDeath = actionLog.getLog(roomId).stream()
                .anyMatch(e -> e.content() != null && e.content().startsWith("NIGHT_DEATHS"));
        if (hasNightDeath) {
            assertThat(user).containsAnyOf("昨夜死亡", "昨夜平安");
        }
        boolean hadVote = actionLog.getLog(roomId).stream()
                .anyMatch(e -> e.action() == com.werewolfengine.game.model.GameActionType.VOTE);
        if (hadVote) {
            assertThat(user).contains("投票");
        }
    }

    @Test
    void m3c_sameRoomWolfSeesWolfChatVillagerDoesNot() {
        when(chatModel.chat(anyList())).thenReturn(llmKillResponse());

        String roomId = "mem_m3c_" + System.nanoTime();
        startRoom(roomId);
        assertThat(advanceUntil(roomId, r ->
                actionLog.getLog(roomId).stream()
                        .anyMatch(e -> e.action() == com.werewolfengine.game.model.GameActionType.WOLF_CHAT)))
                .isTrue();

        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        int wolf = room.aliveWolfIds().getFirst();
        int villager = room.alivePlayerIds().stream()
                .filter(id -> room.getPlayer(id).getRole() != Role.WEREWOLF)
                .findFirst()
                .orElseThrow();

        List<ActionLogEntry> log = actionLog.getLog(roomId);
        long wolfChatCount = SeatPerceptionProjector.project(room, wolf, log).events().stream()
                .filter(e -> e.kind() == PerceptionEventKind.WOLF_CHAT)
                .count();
        long villagerWolfChatCount = SeatPerceptionProjector.project(room, villager, log).events().stream()
                .filter(e -> e.kind() == PerceptionEventKind.WOLF_CHAT)
                .count();

        assertThat(wolfChatCount).isGreaterThanOrEqualTo(1);
        assertThat(villagerWolfChatCount).isZero();
    }

    @Test
    void m3d_deadSeatSkipsDecideButStillProjectsPublicDeaths() {
        String roomId = "mem_m3d_" + System.nanoTime();
        startRoom(roomId);
        assertThat(advanceUntil(roomId, r -> r.getPhase() == GamePhase.DAY_DISCUSS))
                .isTrue();

        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        int deadSeat = room.alivePlayerIds().getFirst();
        room.getPlayer(deadSeat).setAlive(false);

        assertThat(aiService.decide(room, deadSeat)).isEmpty();

        List<ActionLogEntry> log = actionLog.getLog(roomId);
        SeatPerceptionSlice slice = SeatPerceptionProjector.project(room, deadSeat, log);
        if (log.stream().anyMatch(e -> e.content() != null && e.content().startsWith("NIGHT_DEATHS"))) {
            assertThat(slice.events()).extracting(e -> e.kind())
                    .contains(PerceptionEventKind.NIGHT_DEATH);
        }
    }

    private void startRoom(String roomId) {
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        assertThat(stateMachine.startGame(roomId).success()).isTrue();
    }

    private boolean advanceUntil(String roomId, Predicate<GameRoomState> done) {
        MockGameRunner runner = new MockGameRunner(stateMachine, coordinator);
        for (int i = 0; i < MockGameRunner.DEFAULT_MAX_STEPS; i++) {
            GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
            if (done.test(room)) {
                return true;
            }
            if (room.getPhase() == GamePhase.GAME_OVER) {
                return false;
            }
            if (!coordinator.tickOneStep(roomId, room)) {
                return false;
            }
        }
        return false;
    }

    private static ChatResponse llmSkipSpeakResponse() {
        return ChatResponse.builder()
                .aiMessage(dev.langchain4j.data.message.AiMessage.from(
                        "{\"action\":\"SKIP_SPEAK\",\"reason\":\"mock\"}"))
                .build();
    }

    private static ChatResponse llmKillResponse() {
        return ChatResponse.builder()
                .aiMessage(dev.langchain4j.data.message.AiMessage.from(
                        "{\"action\":\"KILL\",\"target\":5,\"reason\":\"mock\"}"))
                .build();
    }

    private static String userText(List<ChatMessage> messages) {
        return messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(m -> ((UserMessage) m).singleText())
                .findFirst()
                .orElse("");
    }
}

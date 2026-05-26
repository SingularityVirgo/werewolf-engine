package com.werewolfengine.ai.api;

import com.werewolfengine.ai.config.AiProperties;
import com.werewolfengine.ai.guard.AiLegalActions;
import com.werewolfengine.ai.memory.MemoryPromptFormatter;
import com.werewolfengine.ai.parse.AiIntentParser;
import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.ai.prompt.AiPromptBuilder;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIServiceRetryTest {

    private GameStateMachine stateMachine;
    private ChatModel chatModel;
    private AIService aiService;

    @BeforeEach
    void setUp() {
        stateMachine = new GameStateMachine();
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setWolvesOnly(false);
        properties.setMaxLlmRetries(2);
        chatModel = mock(ChatModel.class);
        aiService = new AIService(
                properties,
                new MockAIPlayer(),
                new AiPromptBuilder(),
                new MemoryPromptFormatter(),
                new AiIntentParser(),
                new AiLegalActions(),
                chatModel,
                null
        );
    }

    @Test
    void succeedsOnSecondLlmCallAfterParseFailure() {
        when(chatModel.chat(anyList()))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("not-json")).build())
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("{\"action\":\"KILL\",\"target\":5,\"reason\":\"刀\"}"))
                        .build());

        GameRoomState room = startWolfRoom("retry_ok");
        int wolf = mockKillWolfSeat(room);
        Optional<DecisionResult> result = aiService.decideWithSource(room, wolf);

        assertThat(result).isPresent();
        assertThat(result.get().source()).isEqualTo(DecisionResult.Source.LLM);
        assertThat(result.get().intent().action()).isEqualTo(GameActionType.KILL);
        verify(chatModel, times(2)).chat(anyList());
    }

    @Test
    void fallsBackToMockAfterMaxParseRetries() {
        when(chatModel.chat(anyList())).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("not-json"))
                .build());

        GameRoomState room = startWolfRoom("retry_fail");
        int wolf = mockKillWolfSeat(room);
        Optional<DecisionResult> result = aiService.decideWithSource(room, wolf);

        assertThat(result).isPresent();
        assertThat(result.get().source()).isEqualTo(DecisionResult.Source.MOCK_FALLBACK);
        verify(chatModel, times(3)).chat(anyList());
    }

    private GameRoomState startWolfRoom(String roomId) {
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        stateMachine.startGame(roomId);
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        assertThat(room.getPhase()).isEqualTo(GamePhase.NIGHT_WOLF);
        return room;
    }

    private static int mockKillWolfSeat(GameRoomState room) {
        List<Integer> wolves = room.aliveWolfIds().stream().sorted().toList();
        if (wolves.size() >= 2) {
            return wolves.get(1);
        }
        room.setWolfChatInPhase(true);
        return wolves.getFirst();
    }
}

package com.werewolfengine.gateway.session;

import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.common.config.WerewolfGatewayProperties;
import com.werewolfengine.game.chat.ChatMessageService;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.ConnectionState;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.observability.GameActionRecorder;
import com.werewolfengine.game.orchestration.GamePhaseScheduler;
import com.werewolfengine.game.orchestration.PhaseTimeoutHandler;
import com.werewolfengine.game.orchestration.TurnActorResolver;
import com.werewolfengine.game.testsupport.GameTestAiSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisconnectTimeoutHandlerTest {

    @Test
    void graceExpiryMarksSeatAiHosted() {
        ActionLogService actionLog = new ActionLogService();
        GameStateMachine stateMachine = new GameStateMachine(actionLog);
        GameEngineService gameEngine = engine(stateMachine, actionLog);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        DisconnectTimeoutHandler handler = new DisconnectTimeoutHandler(
                gameEngine, sessionStore, new WerewolfGatewayProperties());

        String roomId = "r_grace";
        GameRoomState room = stateMachine.createRoom(roomId);
        room.setStatus(RoomStatus.PLAYING);
        PlayerState seat = room.getPlayer(1);
        seat.setHumanUserId(1001L);
        gameEngine.markSeatGrace(roomId, 1, System.currentTimeMillis() - 1);

        handler.processRoom(roomId);

        assertThat(seat.getConnectionState()).isEqualTo(ConnectionState.AI_HOSTED);
        assertThat(sessionStore.isInGrace(roomId, 1)).isFalse();
    }

    @Test
    void schedulerTickInvokesGraceProcessor() {
        ActionLogService actionLog = new ActionLogService();
        GameStateMachine stateMachine = new GameStateMachine(actionLog);
        var harness = GameTestAiSupport.mockOnly(stateMachine, actionLog);
        GameEngineService gameEngine = engine(stateMachine, actionLog);
        DisconnectTimeoutHandler handler = new DisconnectTimeoutHandler(
                gameEngine, new InMemorySessionStore(), new WerewolfGatewayProperties());

        String roomId = "r_tick_grace";
        GameRoomState room = stateMachine.createRoom(roomId);
        room.setStatus(RoomStatus.PLAYING);
        room.setPhase(GamePhase.NIGHT_WOLF);
        PlayerState seat = room.getPlayer(1);
        seat.setHumanUserId(1001L);
        gameEngine.markSeatGrace(roomId, 1, System.currentTimeMillis() - 1000);

        GameActionRecorder recorder = new GameActionRecorder(stateMachine, actionLog);
        GamePhaseScheduler scheduler = new GamePhaseScheduler(
                stateMachine,
                harness.turnCoordinator(),
                new PhaseTimeoutHandler(
                        stateMachine,
                        new TurnActorResolver(),
                        GameTestAiSupport.disabledAiService(),
                        new MockAIPlayer(),
                        actionLog,
                        recorder),
                recorder,
                handler
        );
        scheduler.tick(roomId);

        assertThat(seat.getConnectionState()).isEqualTo(ConnectionState.AI_HOSTED);
    }

    private static GameEngineService engine(GameStateMachine stateMachine, ActionLogService actionLog) {
        return new GameEngineService(
                stateMachine,
                null,
                null,
                actionLog,
                null,
                new ChatMessageService(),
                new GameActionRecorder(stateMachine, actionLog)
        );
    }
}

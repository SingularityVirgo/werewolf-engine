package com.werewolfengine.game;

import com.werewolfengine.game.model.ActionErrorCode;
import com.werewolfengine.game.model.GameActionCommand;
import com.werewolfengine.game.model.GameActionType;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateMachineTest {

    private GameStateMachine sm;

    @BeforeEach
    void setUp() {
        sm = new GameStateMachine();
    }

    @Test
    void startGame_advancesToNightWolf() {
        String roomId = "r_test";
        sm.createRoom(roomId);
        sm.markAllReady(roomId);

        GameStateMachine.StartGameResult result = sm.startGame(roomId);

        assertThat(result.success()).isTrue();
        assertThat(result.phase()).isEqualTo(GamePhase.NIGHT_WOLF);

        GameRoomState room = sm.getRoom(roomId).orElseThrow();
        assertThat(room.getPhase()).isEqualTo(GamePhase.NIGHT_WOLF);
        assertThat(room.getRound()).isEqualTo(1);
        assertThat(room.isWolfChatInPhase()).isFalse();
        assertThat(room.aliveWolfIds()).hasSize(4);
    }

    @Test
    void killOnNonWolf_succeedsWithoutWolfChat() {
        String roomId = "r_kill";
        sm.createRoom(roomId);
        sm.markAllReady(roomId);
        sm.startGame(roomId);
        GameRoomState room = sm.getRoom(roomId).orElseThrow();

        int wolf = room.aliveWolfIds().getFirst();
        int target = room.alivePlayerIds().stream()
                .filter(id -> room.getPlayer(id).getRole() != Role.WEREWOLF)
                .findFirst()
                .orElseThrow();

        GameStateMachine.HandleActionResult result = sm.handleAction(roomId,
                new GameActionCommand(wolf, GameActionType.KILL, target, GamePhase.NIGHT_WOLF));

        assertThat(result.ack().success()).isTrue();
        assertThat(room.getWolfKillVotes()).containsEntry(wolf, target);
    }

    @Test
    void killOnWolf_withoutChat_returnsWolfChatRequired() {
        String roomId = "r_r17a";
        sm.createRoom(roomId);
        sm.markAllReady(roomId);
        sm.startGame(roomId);
        GameRoomState room = sm.getRoom(roomId).orElseThrow();

        int wolf = room.aliveWolfIds().getFirst();
        int wolfTarget = room.aliveWolfIds().get(1);

        GameStateMachine.HandleActionResult result = sm.handleAction(roomId,
                new GameActionCommand(wolf, GameActionType.KILL, wolfTarget, GamePhase.NIGHT_WOLF));

        assertThat(result.ack().success()).isFalse();
        assertThat(result.ack().code()).isEqualTo(ActionErrorCode.WOLF_CHAT_REQUIRED);
    }

    @Test
    void killOnWolf_afterWolfChat_succeeds() {
        String roomId = "r_chat";
        sm.createRoom(roomId);
        sm.markAllReady(roomId);
        sm.startGame(roomId);
        GameRoomState room = sm.getRoom(roomId).orElseThrow();

        int wolf = room.aliveWolfIds().getFirst();
        int wolfTarget = room.aliveWolfIds().get(1);

        sm.handleAction(roomId, new GameActionCommand(wolf, GameActionType.WOLF_CHAT, null, GamePhase.NIGHT_WOLF));
        GameStateMachine.HandleActionResult result = sm.handleAction(roomId,
                new GameActionCommand(wolf, GameActionType.KILL, wolfTarget, GamePhase.NIGHT_WOLF));

        assertThat(result.ack().success()).isTrue();
        assertThat(room.isWolfChatInPhase()).isTrue();
        assertThat(room.getWolfKillVotes()).containsEntry(wolf, wolfTarget);
    }
}

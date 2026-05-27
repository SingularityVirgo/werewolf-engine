package com.werewolfengine.game.orchestration;

import com.werewolfengine.ai.policy.MockAIPlayer;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.game.observability.GameActionRecorder;
import com.werewolfengine.game.testsupport.GameTestAiSupport;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.sync.PhaseCountdown;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseTimeoutHandlerTest {

    private GameStateMachine stateMachine;
    private PhaseTimeoutHandler handler;

    @BeforeEach
    void setUp() {
        PhaseCountdown.setEnabled(true);
        stateMachine = new GameStateMachine();
        ActionLogService actionLog = new ActionLogService();
        handler = new PhaseTimeoutHandler(
                stateMachine,
                new TurnActorResolver(),
                GameTestAiSupport.disabledAiService(),
                new MockAIPlayer(),
                actionLog,
                new GameActionRecorder(stateMachine, actionLog)
        );
    }

    @AfterEach
    void restore() {
        PhaseCountdown.setEnabled(true);
    }

    @Test
    void wolfPhaseTimeout_withNoVotes_resolvesRandomNonWolfAndAdvances() {
        String roomId = "r_wolf_timeout";
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        stateMachine.startGame(roomId);
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        assertThat(room.getPhase()).isEqualTo(GamePhase.NIGHT_WOLF);
        assertThat(room.getWolfKillVotes()).isEmpty();

        expirePhase(room);

        assertThat(handler.applyIfExpired(roomId, room)).isTrue();
        room = stateMachine.getRoom(roomId).orElseThrow();
        assertThat(room.getPhase()).isEqualTo(GamePhase.NIGHT_SEER);
        assertThat(room.getPendingWolfKillTarget()).isNotNull();
        PlayerState victim = room.getPlayer(room.getPendingWolfKillTarget());
        assertThat(victim).isNotNull();
        assertThat(victim.getRole()).isNotEqualTo(Role.WEREWOLF);
        assertThat(room.getWolfKillVotes()).isEmpty();
    }

    @Test
    void wolfPhaseTimeout_withPartialVotes_resolvesViaR10() {
        String roomId = "r_wolf_partial";
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        stateMachine.startGame(roomId);
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();

        final int killTarget = room.alivePlayerIds().stream()
                .filter(id -> room.getPlayer(id).getRole() != Role.WEREWOLF)
                .findFirst()
                .orElseThrow();
        int wolf = room.aliveWolfIds().getFirst();
        room.getWolfKillVotes().put(wolf, killTarget);

        expirePhase(room);

        assertThat(handler.applyIfExpired(roomId, room)).isTrue();
        GameRoomState after = stateMachine.getRoom(roomId).orElseThrow();
        assertThat(after.getPhase()).isEqualTo(GamePhase.NIGHT_SEER);
        assertThat(after.getPendingWolfKillTarget()).isEqualTo(killTarget);
    }

    @Test
    void dayVoteTimeout_abstainsPendingVotersAndAdvances() {
        String roomId = "r_vote_timeout";
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        stateMachine.startGame(roomId);
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        room.setPhase(GamePhase.DAY_VOTE);
        room.getDayVotes().clear();
        room.getDayVotes().put(1, 2);

        expirePhase(room);

        assertThat(handler.applyIfExpired(roomId, room)).isTrue();
        GameRoomState after = stateMachine.getRoom(roomId).orElseThrow();
        assertThat(after.getPhase()).isNotIn(GamePhase.DAY_VOTE);
    }

    @Test
    void dayDiscussTimeout_skipsCurrentSpeaker() {
        String roomId = "r_discuss_timeout";
        stateMachine.createRoom(roomId);
        stateMachine.markAllReady(roomId);
        stateMachine.startGame(roomId);
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        room.setPhase(GamePhase.DAY_DISCUSS);
        room.getDiscussOrder().clear();
        room.getDiscussOrder().addAll(room.alivePlayerIds());
        room.setDiscussIndex(0);

        int firstSpeaker = room.getDiscussOrder().getFirst();
        expirePhase(room);

        assertThat(handler.applyIfExpired(roomId, room)).isTrue();
        room = stateMachine.getRoom(roomId).orElseThrow();
        assertThat(room.getDiscussIndex()).isEqualTo(1);
        if (room.getPhase() == GamePhase.DAY_DISCUSS) {
            assertThat(room.getDiscussOrder().get(room.getDiscussIndex())).isNotEqualTo(firstSpeaker);
        }
    }

    @Test
    void applyIfExpired_noOpWhenTimerStillActive() {
        String roomId = "r_active";
        stateMachine.createRoom(roomId);
        GameRoomState room = stateMachine.getRoom(roomId).orElseThrow();
        room.setPhase(GamePhase.NIGHT_WOLF);
        PhaseCountdown.onPhaseOrTurnEntered(room);

        assertThat(handler.applyIfExpired(roomId, room)).isFalse();
    }

    private static void expirePhase(GameRoomState room) {
        room.setPhaseDeadlineEpochMs(System.currentTimeMillis() - 1);
    }
}

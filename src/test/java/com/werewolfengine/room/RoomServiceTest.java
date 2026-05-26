package com.werewolfengine.room;

import com.werewolfengine.game.chat.ChatMessageService;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.gateway.ConnectionManager;
import com.werewolfengine.gateway.RoomExecutionGuard;
import com.werewolfengine.gateway.RoomPhaseTickScheduler;
import com.werewolfengine.gateway.WsPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private WsPushService wsPushService;

    @Mock
    private RoomPhaseTickScheduler phaseTickScheduler;

    @Mock
    private ConnectionManager connectionManager;

    private GameEngineService gameEngine;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngineService(
                new GameStateMachine(new ActionLogService()),
                null,
                null,
                new ActionLogService(),
                null,
                new ChatMessageService()
        );
        roomService = new RoomService(
                gameEngine,
                new RoomExecutionGuard(),
                wsPushService,
                phaseTickScheduler,
                connectionManager
        );
    }

    @Test
    void createRoom_exposesAiCountAndHost() {
        RoomService.RoomSnapshot snap = roomService.createRoom(null, 1001L, 11);
        assertThat(snap.aiCount()).isEqualTo(11);
        assertThat(snap.hostUserId()).isEqualTo(1001L);
        assertThat(snap.humanCount()).isEqualTo(1);
        assertThat(snap.boardType()).isEqualTo(BoardTypes.STANDARD_12_PRYH_IDIOT);
        assertThat(snap.maxPlayers()).isEqualTo(12);
        assertThat(snap.seats()).hasSize(12);
        assertThat(snap.seats().get(0).seatId()).isEqualTo(1);
        assertThat(snap.seats().get(0).userId()).isEqualTo(1001L);
    }

    @Test
    void createRoom_rejectsUnknownBoardType() {
        assertThatThrownBy(() -> roomService.createRoom("r_bad", null, 0, "UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void join_autoAssignsFirstHumanSeat() {
        RoomService.RoomSnapshot created = roomService.createRoom("r_auto", null, 11);
        RoomService.SeatSnapshot seat = roomService.joinRoom(created.roomId(), null, 42L);
        assertThat(seat.seatId()).isEqualTo(1);
        assertThat(seat.userId()).isEqualTo(42L);
    }

    @Test
    void join_rejectsAiReservedSeat() {
        RoomService.RoomSnapshot created = roomService.createRoom("r_ai", null, 11);
        assertThatThrownBy(() -> roomService.joinRoom(created.roomId(), 12, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI");
    }

    @Test
    void start_withOneHumanAndElevenAi() {
        RoomService.RoomSnapshot created = roomService.createRoom("r_start", 1001L, 11);
        roomService.joinRoom(created.roomId(), 1, 1001L);
        roomService.setReady(created.roomId(), 1, true);
        var result = roomService.startRoom(created.roomId(), 1001L);
        assertThat(result.success()).isTrue();
        assertThat(gameEngine.getRoomState(created.roomId()).getStatus()).isEqualTo(RoomStatus.PLAYING);
    }

    @Test
    void leave_clearsSeatInWaiting() {
        RoomService.RoomSnapshot created = roomService.createRoom("r_leave", null, 0);
        roomService.joinRoom(created.roomId(), 3, 7L);
        roomService.setReady(created.roomId(), 3, true);
        RoomService.SeatSnapshot left = roomService.leaveRoom(created.roomId(), 3, 7L);
        assertThat(left.userId()).isNull();
        assertThat(left.ready()).isFalse();
        RoomService.RoomSnapshot snap = roomService.snapshot(created.roomId());
        assertThat(snap.seats().get(2).userId()).isNull();
    }

    @Test
    void dissolve_removesRoom() {
        RoomService.RoomSnapshot created = roomService.createRoom("r_del", 1L, 0);
        roomService.dissolveRoom(created.roomId(), 1L);
        verify(phaseTickScheduler).stop(created.roomId());
        verify(connectionManager).removeRoom(created.roomId());
        assertThatThrownBy(() -> gameEngine.getRoomState(created.roomId()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

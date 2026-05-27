package com.werewolfengine.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.event.OutboundAudience;
import com.werewolfengine.game.event.OutboundMessage;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.message.payload.PhaseSyncPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WsPushServiceTest {

    @Mock
    private GameEngineService gameEngine;

    private ConnectionManager connectionManager;
    private WsPushService wsPushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
        wsPushService = new WsPushService(connectionManager, gameEngine);
    }

    @Test
    void pushPhaseSync_sendsTargetedEnvelope() throws Exception {
        String roomId = "r_push";
        int seatId = 3;
        WebSocketSession session = openSession("sess-1");
        connectionManager.register(session, null);
        connectionManager.bind(session.getId(), roomId, seatId);

        PhaseSyncPayload sync = sampleSync(GamePhase.NIGHT_WOLF);
        when(gameEngine.buildPhaseSync(roomId, seatId)).thenReturn(sync);

        wsPushService.pushPhaseSync(roomId, seatId);

        TextMessage sent = captureLastMessage(session);
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = objectMapper.readValue(sent.getPayload(), Map.class);
        assertThat(envelope.get("type")).isEqualTo("PHASE_SYNC");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("seatId")).isEqualTo(3);
        assertThat(payload.get("phaseSync")).isNotNull();
    }

    @Test
    void deliver_wolfChat_onlyReachesAliveWolves() throws Exception {
        String roomId = "r_wolf_chat";
        GameRoomState room = wolfChatRoom(roomId);
        WebSocketSession wolfSession = openSession("wolf");
        WebSocketSession villagerSession = openSession("vil");
        connectionManager.register(wolfSession, null);
        connectionManager.bind(wolfSession.getId(), roomId, 1);
        connectionManager.register(villagerSession, null);
        connectionManager.bind(villagerSession.getId(), roomId, 2);

        OutboundMessage msg = OutboundMessage.chatBroadcast(
                "WEREWOLF", 1, "商议", GamePhase.NIGHT_WOLF, 1);
        wsPushService.deliver(room, msg);

        assertThat(captureAllMessages(wolfSession)).hasSize(1);
        assertThat(captureAllMessages(villagerSession)).isEmpty();
    }

    @Test
    void flushOutbound_drainsQueueAndPushesGameEvent() throws Exception {
        String roomId = "r_evt";
        when(gameEngine.getRoomState(roomId)).thenReturn(sampleRoom(roomId));
        WebSocketSession session = openSession("evt");
        connectionManager.register(session, null);
        connectionManager.bind(session.getId(), roomId, 1);

        GameRoomState room = gameEngine.getRoomState(roomId);
        OutboundMessage.enqueueIdiotRevealed(room, 7);

        wsPushService.flushOutbound(roomId);

        TextMessage sent = captureLastMessage(session);
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = objectMapper.readValue(sent.getPayload(), Map.class);
        assertThat(envelope.get("type")).isEqualTo("GAME_EVENT");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("eventType")).isEqualTo("IDIOT_REVEALED");
        assertThat(room.drainOutbound()).isEmpty();
    }

    @Test
    void shouldReceive_respectsAudience() {
        GameRoomState room = wolfChatRoom("r_aud");
        assertThat(WsPushService.shouldReceive(room, 1, OutboundAudience.WOLF_ONLY)).isTrue();
        assertThat(WsPushService.shouldReceive(room, 2, OutboundAudience.WOLF_ONLY)).isFalse();
        room.getPlayer(2).setAlive(false);
        assertThat(WsPushService.shouldReceive(room, 2, OutboundAudience.ALIVE_CONNECTED)).isFalse();
        assertThat(WsPushService.shouldReceive(room, 2, OutboundAudience.PUBLIC)).isTrue();
    }

    @Test
    void pushPhaseSyncToConnected_onlySameRoomOpenSeats() throws Exception {
        String roomA = "r_a";
        String roomB = "r_b";
        WebSocketSession s1 = openSession("s1");
        WebSocketSession s2 = openSession("s2");
        WebSocketSession s3 = mock(WebSocketSession.class);
        when(s3.getId()).thenReturn("s3");

        connectionManager.register(s1, null);
        connectionManager.bind(s1.getId(), roomA, 1);
        connectionManager.register(s2, null);
        connectionManager.bind(s2.getId(), roomA, 2);
        connectionManager.register(s3, null);
        connectionManager.bind(s3.getId(), roomB, 1);

        when(gameEngine.buildPhaseSync(eq(roomA), any(Integer.class)))
                .thenAnswer(inv -> sampleSync(GamePhase.WAITING));

        wsPushService.pushPhaseSyncToConnected(roomA);

        verify(gameEngine, times(1)).buildPhaseSync(roomA, 1);
        verify(gameEngine, times(1)).buildPhaseSync(roomA, 2);
        verify(gameEngine, never()).buildPhaseSync(eq(roomB), anyInt());

        assertThat(captureAllMessages(s1)).hasSize(1);
        assertThat(captureAllMessages(s2)).hasSize(1);
        verify(s3, never()).sendMessage(any());
    }

    private static GameRoomState wolfChatRoom(String roomId) {
        GameRoomState room = new GameRoomState(roomId);
        room.setStatus(RoomStatus.PLAYING);
        room.setPhase(GamePhase.NIGHT_WOLF);
        room.getPlayer(1).setRole(Role.WEREWOLF);
        room.getPlayer(2).setRole(Role.VILLAGER);
        return room;
    }

    private static GameRoomState sampleRoom(String roomId) {
        GameRoomState room = new GameRoomState(roomId);
        room.setStatus(RoomStatus.PLAYING);
        room.setPhase(GamePhase.DAY_VOTE);
        return room;
    }

    private static PhaseSyncPayload sampleSync(GamePhase phase) {
        return new PhaseSyncPayload(
                phase,
                1,
                null,
                List.of(1, 2, 3),
                null,
                List.of(),
                true,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static WebSocketSession openSession(String id) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private TextMessage captureLastMessage(WebSocketSession session) throws Exception {
        List<TextMessage> messages = captureAllMessages(session);
        assertThat(messages).isNotEmpty();
        return messages.getLast();
    }

    private List<TextMessage> captureAllMessages(WebSocketSession session) throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeast(0)).sendMessage(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }
}

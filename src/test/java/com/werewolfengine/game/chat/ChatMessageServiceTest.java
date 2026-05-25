package com.werewolfengine.game.chat;

import com.werewolfengine.game.engine.GameStateMachine;
import com.werewolfengine.game.event.OutboundMessage;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.Role;
import com.werewolfengine.game.observability.ActionLogService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageServiceTest {

    private final ChatMessageService chat = new ChatMessageService();

    @Test
    void wolfChat_setsFlagAndQueuesBroadcast() {
        GameRoomState room = playingRoom();
        room.setPhase(GamePhase.NIGHT_WOLF);
        PlayerState wolf = room.getPlayer(1);
        wolf.setRole(Role.WEREWOLF);

        var result = chat.handle(room, 1, "WEREWOLF", "先刀8");

        assertThat(result.success()).isTrue();
        assertThat(room.isWolfChatInPhase()).isTrue();
        assertThat(room.drainOutbound()).singleElement()
                .satisfies(m -> {
                    assertThat(m.kind()).isEqualTo(OutboundMessage.OutboundKind.CHAT_BROADCAST);
                    assertThat(m.data().get("scope")).isEqualTo("WEREWOLF");
                    assertThat(m.data().get("content")).isEqualTo("先刀8");
                });
    }

    @Test
    void publicChat_rejectsWhenNotSpeaker() {
        GameRoomState room = playingRoom();
        room.setPhase(GamePhase.DAY_DISCUSS);
        room.getDiscussOrder().add(2);
        room.setDiscussIndex(0);

        var result = chat.handle(room, 1, "ALL", "大家好");

        assertThat(result.success()).isFalse();
        assertThat(result.ack().code().name()).isEqualTo("NOT_YOUR_TURN");
    }

    private static GameRoomState playingRoom() {
        GameStateMachine sm = new GameStateMachine(new ActionLogService());
        sm.createRoom("r_chat");
        sm.markAllReady("r_chat");
        sm.startGame("r_chat");
        return sm.getRoom("r_chat").orElseThrow();
    }
}

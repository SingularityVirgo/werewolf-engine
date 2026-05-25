package com.werewolfengine.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionManagerTest {

    @Test
    void connectedSeatIds_returnsOpenBoundSeatsInRoom() {
        ConnectionManager cm = new ConnectionManager();
        WebSocketSession open = mock(WebSocketSession.class);
        when(open.getId()).thenReturn("open");
        when(open.isOpen()).thenReturn(true);

        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.getId()).thenReturn("closed");
        when(closed.isOpen()).thenReturn(false);

        WebSocketSession otherRoom = mock(WebSocketSession.class);
        when(otherRoom.getId()).thenReturn("other");
        when(otherRoom.isOpen()).thenReturn(true);

        cm.register(open);
        cm.bind("open", "r1", 1);
        cm.register(closed);
        cm.bind("closed", "r1", 2);
        cm.register(otherRoom);
        cm.bind("other", "r2", 5);

        List<Integer> seats = cm.connectedSeatIds("r1");
        assertThat(seats).containsExactly(1);
    }

    @Test
    void bind_replacesPreviousSeatMapping() {
        ConnectionManager cm = new ConnectionManager();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);

        cm.register(session);
        cm.bind("s1", "r1", 1);
        cm.bind("s1", "r1", 2);

        assertThat(cm.findBySeat("r1", 1)).isEmpty();
        assertThat(cm.findBySeat("r1", 2)).isPresent();
        assertThat(cm.connectedSeatIds("r1")).containsExactly(2);
    }
}

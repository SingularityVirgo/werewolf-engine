package com.werewolfengine.gateway.session;

import com.werewolfengine.common.config.WerewolfGatewayProperties;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.model.ConnectionState;
import com.werewolfengine.game.engine.GameEngineService.UserSeatBinding;
import com.werewolfengine.gateway.ConnectionManager;
import com.werewolfengine.gateway.WsPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** 30s reconnect window (ADR-007 §5.3-C). */
@Service
public class ReconnectService {

    private static final Logger log = LoggerFactory.getLogger(ReconnectService.class);

    private final GameEngineService gameEngine;
    private final SessionStore sessionStore;
    private final ConnectionManager connectionManager;
    private final WsPushService wsPushService;
    private final WerewolfGatewayProperties gatewayProperties;

    public ReconnectService(
            @Lazy GameEngineService gameEngine,
            SessionStore sessionStore,
            ConnectionManager connectionManager,
            WsPushService wsPushService,
            WerewolfGatewayProperties gatewayProperties
    ) {
        this.gameEngine = gameEngine;
        this.sessionStore = sessionStore;
        this.connectionManager = connectionManager;
        this.wsPushService = wsPushService;
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * @return true when an in-progress seat was restored without JOIN_ROOM
     */
    public boolean tryReconnect(WebSocketSession session, long userId) {
        Optional<UserSeatBinding> seat = gameEngine.findPlayingSeatForUser(userId);
        if (seat.isEmpty()) {
            return false;
        }
        UserSeatBinding binding = seat.get();
        String roomId = binding.roomId();
        int playerId = binding.playerId();
        if (binding.connectionState() == ConnectionState.AI_HOSTED) {
            return false;
        }
        if (!sessionStore.isInGrace(roomId, playerId)
                && binding.connectionState() != ConnectionState.GRACE) {
            return false;
        }
        sessionStore.clearGrace(roomId, playerId);
        sessionStore.bindConnection(roomId, playerId, session.getId());
        connectionManager.rebindSession(session, roomId, playerId);
        gameEngine.markSeatOnline(roomId, playerId);
        wsPushService.sendEnvelope(session, Map.of(
                "type", "JOIN_ROOM",
                "payload", Map.of(
                        "roomId", roomId,
                        "seatId", playerId,
                        "playerId", playerId,
                        "reconnected", true
                )
        ));
        wsPushService.pushPhaseSync(roomId, playerId);
        log.info("reconnected userId={} roomId={} playerId={}", userId, roomId, playerId);
        return true;
    }

    public void onPlayingDisconnect(String sessionId, String roomId, int playerId, long userId) {
        Duration ttl = Duration.ofSeconds(Math.max(1, gatewayProperties.getReconnectGraceSeconds()));
        long now = System.currentTimeMillis();
        sessionStore.setGrace(roomId, playerId, now, ttl);
        gameEngine.markSeatGrace(roomId, playerId, now + ttl.toMillis());
        connectionManager.graceUnlink(sessionId);
        log.info("grace started roomId={} playerId={} userId={} ttlSec={}",
                roomId, playerId, userId, ttl.toSeconds());
    }

    public void bindJoinedSeat(WebSocketSession session, String roomId, int playerId) {
        sessionStore.bindConnection(roomId, playerId, session.getId());
        sessionStore.addRoomPlayer(roomId, playerId);
        connectionManager.bind(session.getId(), roomId, playerId);
    }
}

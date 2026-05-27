package com.werewolfengine.gateway.session;

import com.werewolfengine.common.config.WerewolfGatewayProperties;
import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.model.ConnectionState;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.RoomStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Grace expiry → AI_HOSTED (ADR-007 §5.3-D). */
@Service
public class DisconnectTimeoutHandler implements DisconnectGraceProcessor {

    private static final Logger log = LoggerFactory.getLogger(DisconnectTimeoutHandler.class);

    private final GameEngineService gameEngine;
    private final SessionStore sessionStore;
    private final WerewolfGatewayProperties gatewayProperties;

    public DisconnectTimeoutHandler(
            @Lazy GameEngineService gameEngine,
            SessionStore sessionStore,
            WerewolfGatewayProperties gatewayProperties
    ) {
        this.gameEngine = gameEngine;
        this.sessionStore = sessionStore;
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public void processRoom(String roomId) {
        GameRoomState room;
        try {
            room = gameEngine.getRoomState(roomId);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (room.getStatus() != RoomStatus.PLAYING) {
            return;
        }
        long now = System.currentTimeMillis();
        for (PlayerState player : room.getPlayers().values()) {
            if (player.getHumanUserId() == null || player.getConnectionState() != ConnectionState.GRACE) {
                continue;
            }
            Long deadline = player.getGraceDeadlineMs();
            boolean graceActive = sessionStore.isInGrace(roomId, player.getPlayerId());
            boolean deadlinePassed = deadline != null && now >= deadline;
            if (deadlinePassed || !graceActive) {
                markAiHosted(roomId, player.getPlayerId());
            }
        }
    }

    private void markAiHosted(String roomId, int playerId) {
        gameEngine.markSeatAiHosted(roomId, playerId);
        sessionStore.clearGrace(roomId, playerId);
        log.info("grace expired → AI_HOSTED roomId={} playerId={}", roomId, playerId);
    }

    public Duration graceTtl() {
        return Duration.ofSeconds(Math.max(1, gatewayProperties.getReconnectGraceSeconds()));
    }
}

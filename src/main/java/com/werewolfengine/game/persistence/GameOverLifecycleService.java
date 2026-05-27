package com.werewolfengine.game.persistence;

import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.model.GamePhase;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.room.RoomService;
import com.werewolfengine.room.persistence.RoomLobbyPersistence;
import com.werewolfengine.gateway.session.GameSessionCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Post-{@link GamePhase#GAME_OVER} archive + memory cleanup (ADR-007 phase 2).
 */
@Service
public class GameOverLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(GameOverLifecycleService.class);

    private final GameEngineService gameEngine;
    private final RoomService roomService;
    private final RoomLobbyPersistence lobbyPersistence;
    private final ObjectProvider<GameArchiveService> gameArchiveService;
    private final ObjectProvider<GameSessionCleanupService> sessionCleanup;
    private final ConcurrentHashMap<String, Boolean> finalized = new ConcurrentHashMap<>();

    public GameOverLifecycleService(
            GameEngineService gameEngine,
            @Lazy RoomService roomService,
            RoomLobbyPersistence lobbyPersistence,
            ObjectProvider<GameArchiveService> gameArchiveService,
            ObjectProvider<GameSessionCleanupService> sessionCleanup
    ) {
        this.gameEngine = gameEngine;
        this.roomService = roomService;
        this.lobbyPersistence = lobbyPersistence;
        this.gameArchiveService = gameArchiveService;
        this.sessionCleanup = sessionCleanup;
    }

    public void finalizeIfGameOver(String roomId) {
        GameRoomState room;
        try {
            room = gameEngine.getRoomState(roomId);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (room.getPhase() != GamePhase.GAME_OVER) {
            return;
        }
        if (finalized.putIfAbsent(roomId, Boolean.TRUE) != null) {
            return;
        }
        try {
            gameArchiveService.ifAvailable(archive -> archive.archiveGameOver(roomId, room));
            if (gameArchiveService.getIfAvailable() == null) {
                lobbyPersistence.onGameEnded(roomId);
            }
            roomService.evictAfterGameOver(roomId);
            sessionCleanup.ifAvailable(cleanup -> cleanup.cleanupRoom(roomId));
            gameEngine.removeRoom(roomId);
        } catch (RuntimeException e) {
            finalized.remove(roomId);
            log.error("GAME_OVER finalize failed roomId={}: {}", roomId, e.getMessage(), e);
            throw e;
        }
    }
}

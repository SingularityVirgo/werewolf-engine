package com.werewolfengine.gateway.session;

import org.springframework.stereotype.Service;

/** Clears per-room Redis / memory session keys on GAME_OVER (ADR-007 §4.2). */
@Service
public class GameSessionCleanupService {

    private final SessionStore sessionStore;

    public GameSessionCleanupService(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public void cleanupRoom(String roomId) {
        sessionStore.cleanupRoom(roomId);
    }
}

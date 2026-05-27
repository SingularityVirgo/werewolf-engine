package com.werewolfengine.gateway.session;

/** Scans GRACE seats each phase tick (ADR-007 §5.3-D). */
public interface DisconnectGraceProcessor {

    void processRoom(String roomId);
}

package com.werewolfengine.gateway;

import com.werewolfengine.game.engine.GameEngineService;
import com.werewolfengine.game.orchestration.GamePhaseScheduler;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Per-room phase tick for Formal path B (ADR-005 P-04): calls {@link GameEngineService#tickPhase}
 * under {@link RoomExecutionGuard}, then pushes {@code PHASE_SYNC} to connected seats.
 */
@Component
public class RoomPhaseTickScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoomPhaseTickScheduler.class);

    private final GameEngineService gameEngine;
    private final RoomExecutionGuard roomGuard;
    private final WsPushService wsPushService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "room-phase-tick");
                t.setDaemon(true);
                return t;
            }
    );
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    @Value("${werewolf.gateway.phase-tick-enabled:true}")
    private boolean enabled;

    @Value("${werewolf.gateway.phase-tick-interval-ms:1500}")
    private long intervalMs;

    public RoomPhaseTickScheduler(
            GameEngineService gameEngine,
            RoomExecutionGuard roomGuard,
            WsPushService wsPushService
    ) {
        this.gameEngine = gameEngine;
        this.roomGuard = roomGuard;
        this.wsPushService = wsPushService;
    }

    public void start(String roomId) {
        if (!enabled) {
            return;
        }
        stop(roomId);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                () -> safeTick(roomId),
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
        );
        tasks.put(roomId, future);
        log.debug("phase tick scheduler started roomId={} intervalMs={}", roomId, intervalMs);
    }

    public void stop(String roomId) {
        ScheduledFuture<?> future = tasks.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Single tick (HTTP/WS manual or scheduled). Returns tick result; pushes on state change.
     */
    public GamePhaseScheduler.TickResult tickOnce(String roomId) {
        return roomGuard.execute(roomId, () -> {
            GamePhaseScheduler.TickResult result = gameEngine.tickPhase(roomId);
            if (shouldPushAfterTick(result)) {
                wsPushService.pushPhaseSyncToConnected(roomId);
            }
            wsPushService.flushOutbound(roomId);
            if ("GAME_OVER".equals(result.status())) {
                stop(roomId);
            }
            return result;
        });
    }

    public Map<String, Object> tickOnceResponse(String roomId) {
        GamePhaseScheduler.TickResult tick = tickOnce(roomId);
        return Map.of(
                "status", tick.status(),
                "phase", tick.phase(),
                "detail", tick.detail()
        );
    }

    static boolean shouldPushAfterTick(GamePhaseScheduler.TickResult result) {
        return switch (result.status()) {
            case "ADVANCED", "AI_STEP", "GAME_OVER", "COUNTDOWN" -> true;
            default -> false;
        };
    }

    private void safeTick(String roomId) {
        try {
            tickOnce(roomId);
        } catch (IllegalArgumentException e) {
            log.debug("stopping phase tick for missing room {}: {}", roomId, e.getMessage());
            stop(roomId);
        } catch (Exception e) {
            log.warn("phase tick failed roomId={}: {}", roomId, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        tasks.keySet().forEach(this::stop);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}

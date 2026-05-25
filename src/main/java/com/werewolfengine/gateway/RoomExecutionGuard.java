package com.werewolfengine.gateway;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Serializes mutating operations per room (ADR-005 §5).
 */
@Component
public class RoomExecutionGuard {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public <T> T execute(String roomId, Supplier<T> task) {
        Object lock = locks.computeIfAbsent(roomId, k -> new Object());
        synchronized (lock) {
            return task.get();
        }
    }

    public void run(String roomId, Runnable task) {
        execute(roomId, () -> {
            task.run();
            return null;
        });
    }
}

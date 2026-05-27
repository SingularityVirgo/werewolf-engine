package com.werewolfengine.gateway.session;

import com.werewolfengine.common.redis.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisSessionStore implements SessionStore {

    private final StringRedisTemplate redis;
    private final InMemorySessionStore fallback;

    public RedisSessionStore(StringRedisTemplate redis, InMemorySessionStore fallback) {
        this.redis = redis;
        this.fallback = fallback;
    }

    @Override
    public void bindConnection(String roomId, int playerId, String sessionId) {
        redis.opsForValue().set(RedisKeys.wsConn(roomId, playerId), sessionId);
        fallback.bindConnection(roomId, playerId, sessionId);
    }

    @Override
    public void setGrace(String roomId, int playerId, long epochMs, Duration ttl) {
        redis.opsForValue().set(
                RedisKeys.wsGrace(roomId, playerId),
                String.valueOf(epochMs),
                ttl
        );
        fallback.setGrace(roomId, playerId, epochMs, ttl);
    }

    @Override
    public boolean clearGrace(String roomId, int playerId) {
        redis.delete(RedisKeys.wsGrace(roomId, playerId));
        return fallback.clearGrace(roomId, playerId);
    }

    @Override
    public boolean isInGrace(String roomId, int playerId) {
        String value = redis.opsForValue().get(RedisKeys.wsGrace(roomId, playerId));
        if (value != null) {
            return true;
        }
        return fallback.isInGrace(roomId, playerId);
    }

    @Override
    public Optional<String> getBoundSessionId(String roomId, int playerId) {
        String sessionId = redis.opsForValue().get(RedisKeys.wsConn(roomId, playerId));
        if (sessionId != null) {
            return Optional.of(sessionId);
        }
        return fallback.getBoundSessionId(roomId, playerId);
    }

    @Override
    public void addRoomPlayer(String roomId, int playerId) {
        redis.opsForSet().add(RedisKeys.roomPlayers(roomId), String.valueOf(playerId));
        fallback.addRoomPlayer(roomId, playerId);
    }

    @Override
    public void removeRoomPlayer(String roomId, int playerId) {
        redis.opsForSet().remove(RedisKeys.roomPlayers(roomId), String.valueOf(playerId));
        fallback.removeRoomPlayer(roomId, playerId);
    }

    @Override
    public Set<Integer> roomPlayerIds(String roomId) {
        Set<String> members = redis.opsForSet().members(RedisKeys.roomPlayers(roomId));
        if (members == null || members.isEmpty()) {
            return fallback.roomPlayerIds(roomId);
        }
        return members.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    @Override
    public void cleanupRoom(String roomId) {
        Set<Integer> seats = roomPlayerIds(roomId);
        for (int seatId : seats) {
            redis.delete(RedisKeys.wsConn(roomId, seatId));
            redis.delete(RedisKeys.wsGrace(roomId, seatId));
        }
        redis.delete(RedisKeys.roomPlayers(roomId));
        fallback.cleanupRoom(roomId);
    }
}

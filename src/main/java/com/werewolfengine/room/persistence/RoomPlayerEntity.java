package com.werewolfengine.room.persistence;

import com.werewolfengine.game.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "room_player")
public class RoomPlayerEntity {

    @Embeddable
    public static class RoomPlayerId implements Serializable {

        @Column(name = "room_id", length = 32)
        private String roomId;

        @Column(name = "player_id")
        private int playerId;

        protected RoomPlayerId() {
        }

        public RoomPlayerId(String roomId, int playerId) {
            this.roomId = roomId;
            this.playerId = playerId;
        }

        public String getRoomId() {
            return roomId;
        }

        public int getPlayerId() {
            return playerId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RoomPlayerId that = (RoomPlayerId) o;
            return playerId == that.playerId && Objects.equals(roomId, that.roomId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomId, playerId);
        }
    }

    @EmbeddedId
    private RoomPlayerId id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_alive", nullable = false)
    private boolean alive = true;

    @Column(name = "is_ready", nullable = false)
    private boolean ready;

    @Column(name = "can_vote", nullable = false)
    private boolean canVote = true;

    @Column(name = "idiot_revealed", nullable = false)
    private boolean idiotRevealed;

    @Column(length = 32)
    private String persona;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    protected RoomPlayerEntity() {
    }

    public static RoomPlayerEntity forSeat(String roomId, int playerId, Long userId) {
        RoomPlayerEntity entity = new RoomPlayerEntity();
        entity.id = new RoomPlayerId(roomId, playerId);
        entity.userId = userId;
        return entity;
    }

    public RoomPlayerId getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}

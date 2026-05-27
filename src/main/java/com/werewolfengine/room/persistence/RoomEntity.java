package com.werewolfengine.room.persistence;

import com.werewolfengine.game.model.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "room")
public class RoomEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoomStatus status;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers = 12;

    @Column(name = "ai_count", nullable = false)
    private int aiCount;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "board_type", nullable = false, length = 64)
    private String boardType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected RoomEntity() {
    }

    public static RoomEntity waiting(
            String id,
            Long hostId,
            int aiCount,
            String boardType
    ) {
        RoomEntity entity = new RoomEntity();
        entity.id = id;
        entity.status = RoomStatus.WAITING;
        entity.hostId = hostId;
        entity.aiCount = aiCount;
        entity.boardType = boardType;
        return entity;
    }

    public String getId() {
        return id;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getBoardType() {
        return boardType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}

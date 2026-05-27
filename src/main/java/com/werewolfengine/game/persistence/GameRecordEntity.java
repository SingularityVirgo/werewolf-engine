package com.werewolfengine.game.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "game_record")
public class GameRecordEntity {

    public enum ArchivedWinner {
        WEREWOLF,
        VILLAGER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 32)
    private String roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ArchivedWinner winner;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_log", nullable = false, columnDefinition = "JSON")
    private String actionLogJson;

    @Column(name = "round_count", nullable = false)
    private int roundCount;

    @Column(name = "board_type", nullable = false, length = 64)
    private String boardType;

    protected GameRecordEntity() {
    }

    public static GameRecordEntity create(
            String roomId,
            ArchivedWinner winner,
            Instant startedAt,
            Instant endedAt,
            String actionLogJson,
            int roundCount,
            String boardType
    ) {
        GameRecordEntity entity = new GameRecordEntity();
        entity.roomId = roomId;
        entity.winner = winner;
        entity.startedAt = startedAt;
        entity.endedAt = endedAt;
        entity.actionLogJson = actionLogJson;
        entity.roundCount = roundCount;
        entity.boardType = boardType;
        return entity;
    }
}

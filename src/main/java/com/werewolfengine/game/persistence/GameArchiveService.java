package com.werewolfengine.game.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolfengine.common.config.WerewolfPersistenceProperties;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.GameWinner;
import com.werewolfengine.game.model.RoomStatus;
import com.werewolfengine.game.observability.ActionLogEntry;
import com.werewolfengine.game.observability.ActionLogService;
import com.werewolfengine.room.persistence.RoomEntity;
import com.werewolfengine.room.persistence.RoomPersistenceException;
import com.werewolfengine.room.persistence.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "werewolf.persistence.game-archive", havingValue = "true")
public class GameArchiveService {

    private static final Logger log = LoggerFactory.getLogger(GameArchiveService.class);

    private final GameRecordRepository gameRecordRepository;
    private final RoomRepository roomRepository;
    private final ActionLogService actionLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameArchiveService(
            GameRecordRepository gameRecordRepository,
            RoomRepository roomRepository,
            ActionLogService actionLogService
    ) {
        this.gameRecordRepository = gameRecordRepository;
        this.roomRepository = roomRepository;
        this.actionLogService = actionLogService;
    }

    @Transactional
    public void archiveGameOver(String roomId, GameRoomState room) {
        if (room.getStatus() != RoomStatus.ENDED || room.getWinner() == null) {
            throw new IllegalStateException("Room not ended: " + roomId);
        }
        RoomEntity roomEntity = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomPersistenceException("Room not in MySQL: " + roomId));

        Instant endedAt = Instant.now();
        Instant startedAt = roomEntity.getStartedAt() != null ? roomEntity.getStartedAt() : endedAt;
        String actionLogJson = serializeActionLog(roomId);

        gameRecordRepository.save(GameRecordEntity.create(
                roomId,
                toArchivedWinner(room.getWinner()),
                startedAt,
                endedAt,
                actionLogJson,
                room.getRound(),
                roomEntity.getBoardType()
        ));

        roomEntity.setStatus(RoomStatus.ENDED);
        roomEntity.setEndedAt(endedAt);
        roomRepository.save(roomEntity);

        log.info("Archived game roomId={} winner={} logEntries={}",
                roomId,
                room.getWinner(),
                actionLogService.getLog(roomId).size());
    }

    private String serializeActionLog(String roomId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ActionLogEntry entry : actionLogService.exportForArchive(roomId)) {
            rows.add(toLogRow(entry));
        }
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new RoomPersistenceException("Failed to serialize action_log for " + roomId, e);
        }
    }

    private static Map<String, Object> toLogRow(ActionLogEntry entry) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("round", entry.round());
        row.put("phase", entry.phase() != null ? entry.phase().name() : null);
        row.put("playerId", entry.playerId());
        row.put("role", entry.role() != null ? entry.role().name() : null);
        row.put("action", entry.action() != null ? entry.action().name() : null);
        row.put("target", entry.target());
        row.put("success", entry.success());
        row.put("timestamp", entry.timestamp());
        if (entry.content() != null) {
            row.put("content", entry.content());
        }
        if (entry.thinking() != null) {
            row.put("thinking", entry.thinking());
        }
        if (entry.modelId() != null) {
            row.put("modelId", entry.modelId());
        }
        return row;
    }

    private static GameRecordEntity.ArchivedWinner toArchivedWinner(GameWinner winner) {
        return switch (winner) {
            case WEREWOLVES -> GameRecordEntity.ArchivedWinner.WEREWOLF;
            case VILLAGERS -> GameRecordEntity.ArchivedWinner.VILLAGER;
        };
    }
}

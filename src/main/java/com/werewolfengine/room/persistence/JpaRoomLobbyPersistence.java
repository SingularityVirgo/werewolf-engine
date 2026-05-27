package com.werewolfengine.room.persistence;

import com.werewolfengine.common.config.WerewolfPersistenceProperties;
import com.werewolfengine.game.model.GameRoomState;
import com.werewolfengine.game.model.PlayerState;
import com.werewolfengine.game.model.RoomStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "werewolf.persistence.mysql-room", havingValue = "true")
public class JpaRoomLobbyPersistence implements RoomLobbyPersistence, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JpaRoomLobbyPersistence.class);
    static final long DEFAULT_HOST_USER_ID = 10001L;

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserRepository userRepository;
    private final WerewolfPersistenceProperties properties;

    public JpaRoomLobbyPersistence(
            RoomRepository roomRepository,
            RoomPlayerRepository roomPlayerRepository,
            UserRepository userRepository,
            WerewolfPersistenceProperties properties
    ) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isDevUserSeed()) {
            seedDevUsers();
        }
    }

    private void seedDevUsers() {
        for (long id = 10001L; id <= 10012L; id++) {
            if (!userRepository.existsById(id)) {
                userRepository.save(new UserEntity(id, "player-" + id));
            }
        }
        log.info("Dev user seed complete (10001-10012)");
    }

    @Override
    @Transactional
    public void onRoomCreated(String roomId, Long hostUserId, int aiCount, String boardType) {
        long hostId = resolveHostId(hostUserId);
        ensureUserExists(hostId);
        roomRepository.save(RoomEntity.waiting(roomId, hostId, aiCount, boardType));
    }

    @Override
    @Transactional
    public void onPlayerJoined(String roomId, int seatId, Long userId) {
        if (userId != null) {
            ensureUserExists(userId);
            assertHumanNotAlreadySeated(roomId, seatId, userId);
        }
        RoomPlayerEntity entity = roomPlayerRepository
                .findById(new RoomPlayerEntity.RoomPlayerId(roomId, seatId))
                .orElseGet(() -> RoomPlayerEntity.forSeat(roomId, seatId, userId));
        entity.setUserId(userId);
        entity.setReady(false);
        roomPlayerRepository.save(entity);
    }

    @Override
    @Transactional
    public void onPlayerReady(String roomId, int seatId, boolean ready) {
        RoomPlayerEntity entity = requirePlayer(roomId, seatId);
        entity.setReady(ready);
        roomPlayerRepository.save(entity);
    }

    @Override
    @Transactional
    public void onPlayerLeft(String roomId, int seatId) {
        RoomPlayerEntity entity = requirePlayer(roomId, seatId);
        entity.setUserId(null);
        entity.setReady(false);
        roomPlayerRepository.save(entity);
    }

    @Override
    @Transactional
    public void onGameStarted(String roomId, GameRoomState room) {
        RoomEntity roomEntity = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomPersistenceException("Room not in MySQL: " + roomId));
        Instant now = Instant.now();
        roomEntity.setStatus(RoomStatus.PLAYING);
        roomEntity.setStartedAt(now);
        roomRepository.save(roomEntity);

        for (PlayerState player : room.getPlayers().values()) {
            RoomPlayerEntity.RoomPlayerId pk = new RoomPlayerEntity.RoomPlayerId(roomId, player.getPlayerId());
            RoomPlayerEntity seat = roomPlayerRepository.findById(pk)
                    .orElseGet(() -> RoomPlayerEntity.forSeat(roomId, player.getPlayerId(), player.getHumanUserId()));
            seat.setUserId(player.getHumanUserId());
            seat.setReady(player.isReady());
            seat.setRole(player.getRole());
            seat.setAlive(player.isAlive());
            roomPlayerRepository.save(seat);
        }
    }

    @Override
    @Transactional
    public void onRoomDissolved(String roomId) {
        roomRepository.deleteById(roomId);
    }

    @Override
    @Transactional
    public void onGameEnded(String roomId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setStatus(RoomStatus.ENDED);
            room.setEndedAt(Instant.now());
            roomRepository.save(room);
        });
    }

    private RoomPlayerEntity requirePlayer(String roomId, int seatId) {
        return roomPlayerRepository.findById(new RoomPlayerEntity.RoomPlayerId(roomId, seatId))
                .orElseGet(() -> RoomPlayerEntity.forSeat(roomId, seatId, null));
    }

    private void assertHumanNotAlreadySeated(String roomId, int seatId, Long userId) {
        List<RoomPlayerEntity> seats = roomPlayerRepository.findByIdRoomId(roomId);
        for (RoomPlayerEntity seat : seats) {
            if (seat.getUserId() != null
                    && seat.getUserId().equals(userId)
                    && seat.getId().getPlayerId() != seatId) {
                throw new RoomPersistenceException("User already seated in room " + roomId);
            }
        }
    }

    private void ensureUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            userRepository.save(new UserEntity(userId, "user-" + userId));
        }
    }

    static long resolveHostId(Long hostUserId) {
        return hostUserId != null ? hostUserId : DEFAULT_HOST_USER_ID;
    }
}

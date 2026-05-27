package com.werewolfengine.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayerEntity, RoomPlayerEntity.RoomPlayerId> {

    List<RoomPlayerEntity> findByIdRoomId(String roomId);
}

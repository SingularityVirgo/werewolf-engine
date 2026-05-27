package com.werewolfengine.game.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecordEntity, Long> {
}

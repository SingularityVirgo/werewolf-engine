CREATE TABLE game_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    room_id      VARCHAR(32)  NOT NULL,
    winner       ENUM('WEREWOLF', 'VILLAGER') NOT NULL,
    started_at   TIMESTAMP(3) NOT NULL,
    ended_at     TIMESTAMP(3) NOT NULL,
    action_log   JSON         NOT NULL,
    round_count  INT          NOT NULL,
    board_type   VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_gr_room_ended (room_id, ended_at),
    KEY idx_gr_ended (ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

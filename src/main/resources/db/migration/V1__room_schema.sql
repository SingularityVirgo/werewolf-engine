CREATE TABLE werewolf_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(32)  NOT NULL,
    password_hash   VARCHAR(64)  NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room (
    id           VARCHAR(32)  NOT NULL,
    status       ENUM('WAITING', 'PLAYING', 'ENDED') NOT NULL,
    max_players  INT          NOT NULL DEFAULT 12,
    ai_count     INT          NOT NULL DEFAULT 0,
    host_id      BIGINT       NOT NULL,
    board_type   VARCHAR(64)  NOT NULL DEFAULT 'STANDARD_12_PRYH_IDIOT',
    created_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    started_at   TIMESTAMP(3) NULL,
    ended_at     TIMESTAMP(3) NULL,
    PRIMARY KEY (id),
    KEY idx_room_status_created (status, created_at),
    KEY idx_room_host (host_id),
    CONSTRAINT fk_room_host FOREIGN KEY (host_id) REFERENCES werewolf_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room_player (
    room_id         VARCHAR(32)  NOT NULL,
    player_id       INT          NOT NULL,
    user_id         BIGINT       NULL,
    role            ENUM('WEREWOLF', 'VILLAGER', 'IDIOT', 'SEER', 'WITCH', 'HUNTER') NULL,
    is_alive        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_ready        BOOLEAN      NOT NULL DEFAULT FALSE,
    can_vote        BOOLEAN      NOT NULL DEFAULT TRUE,
    idiot_revealed  BOOLEAN      NOT NULL DEFAULT FALSE,
    persona         VARCHAR(32)  NULL,
    joined_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id, player_id),
    KEY idx_rp_room (room_id),
    KEY idx_rp_user (user_id),
    CONSTRAINT fk_rp_room FOREIGN KEY (room_id) REFERENCES room (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_user FOREIGN KEY (user_id) REFERENCES werewolf_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

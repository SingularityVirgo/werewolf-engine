package com.werewolfengine.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "werewolf.persistence")
public class WerewolfPersistenceProperties {

    /** MySQL write-through for room / room_player (ADR-007 phase 1). */
    private boolean mysqlRoom = false;

    /** GAME_OVER archive to game_record (ADR-007 phase 2). */
    private boolean gameArchive = false;

    /** Seed dev users when mysql-room is enabled. */
    private boolean devUserSeed = true;

    /** Redis session + token store (ADR-007 phase 3). */
    private boolean redisSession = false;

    public boolean isMysqlRoom() {
        return mysqlRoom;
    }

    public void setMysqlRoom(boolean mysqlRoom) {
        this.mysqlRoom = mysqlRoom;
    }

    public boolean isGameArchive() {
        return gameArchive;
    }

    public void setGameArchive(boolean gameArchive) {
        this.gameArchive = gameArchive;
    }

    public boolean isDevUserSeed() {
        return devUserSeed;
    }

    public void setDevUserSeed(boolean devUserSeed) {
        this.devUserSeed = devUserSeed;
    }

    public boolean isRedisSession() {
        return redisSession;
    }

    public void setRedisSession(boolean redisSession) {
        this.redisSession = redisSession;
    }
}

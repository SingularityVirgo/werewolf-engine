package com.werewolfengine.room.persistence;

public class RoomPersistenceException extends RuntimeException {

    public RoomPersistenceException(String message) {
        super(message);
    }

    public RoomPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}

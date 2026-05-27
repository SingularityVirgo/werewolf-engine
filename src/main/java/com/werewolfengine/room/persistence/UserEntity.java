package com.werewolfengine.room.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "werewolf_user")
public class UserEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String username;

    @Column(name = "password_hash", length = 64)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UserEntity() {
    }

    public UserEntity(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}

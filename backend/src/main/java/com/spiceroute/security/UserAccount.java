package com.spiceroute.security;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "owner_suspended", nullable = false)
    private boolean ownerSuspended;

    public boolean isOwnerSuspended() { return ownerSuspended; }
    public void setOwnerSuspended(boolean value) { ownerSuspended = value; }

    protected UserAccount() {}

    public UserAccount(UUID id, String name, String email, String passwordHash, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }

    public void rename(String name) { this.name = name; }
    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void setActive(boolean active) { this.active = active; }

    public void toggleActive() {
        active = !active;
    }
}

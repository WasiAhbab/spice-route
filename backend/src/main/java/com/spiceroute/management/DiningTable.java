package com.spiceroute.management;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "restaurant_tables")
public class DiningTable {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(nullable = false)
    private boolean active;

    protected DiningTable() {}

    public DiningTable(UUID id, Branch branch, String label, UUID token) {
        this.id = id;
        this.branch = branch;
        this.label = label;
        this.token = token;
        this.active = true;
    }

    public UUID getId() { return id; }
    public Branch getBranch() { return branch; }
    public String getLabel() { return label; }
    public UUID getToken() { return token; }
    public boolean isActive() { return active; }
    public void rename(String label) { this.label = label; }
    public void setActive(boolean active) { this.active = active; }
    public void rotateToken() { this.token = UUID.randomUUID(); }
    public void toggleActive() { active = !active; }
}

package com.spiceroute.management;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "branches")
public class Branch {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private boolean archived;

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    protected Branch() {}

    public Branch(UUID id, Restaurant restaurant, String name, String address) {
        this.id = id;
        this.restaurant = restaurant;
        this.name = name;
        this.address = address;
    }

    public UUID getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public void update(String name, String address) {
        this.name = name;
        this.address = address;
    }
}

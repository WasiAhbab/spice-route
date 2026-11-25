package com.spiceroute.management;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "restaurants")
public class Restaurant {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean archived;

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    @Column(name = "welcome_message", nullable = false)
    private String welcomeMessage = "";

    @Column(name = "thank_you_message", nullable = false)
    private String thankYouMessage = "";

    @Column(name = "collect_contact", nullable = false)
    private boolean collectContact = true;

    public String getWelcomeMessage() { return welcomeMessage; }
    public String getThankYouMessage() { return thankYouMessage; }
    public boolean isCollectContact() { return collectContact; }

    public void customizeFeedback(String welcome, String thanks, boolean contact) {
        welcomeMessage = welcome;
        thankYouMessage = thanks;
        collectContact = contact;
    }

    protected Restaurant() {}

    public Restaurant(UUID id, String name, UUID ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.active = true;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
    public boolean isActive() { return active; }
    public void rename(String name) { this.name = name; }
    public void toggleActive() { active = !active; }
}

package com.spiceroute.feedback;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "feedback")
public class CustomerFeedback {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id")
    private com.spiceroute.management.DiningTable diningTable;

    @Column(nullable = false)
    private int rating;

    private String comment;

    @Column(name = "customer_name")
    private String customerName;

    private String phone;

    @Column(nullable = false)
    private String language;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "review_status", nullable = false)
    private String reviewStatus = "NEW";

    @Column(name = "internal_note", nullable = false)
    private String internalNote = "";

    @Version
    private long version;

    public UUID getBranchId() { return diningTable.getBranch().getId(); }
    public long getVersion() { return version; }
    public void review(String status, String note) {
        reviewStatus = status;
        internalNote = note;
    }

    protected CustomerFeedback() {}

    public CustomerFeedback(UUID id, com.spiceroute.management.DiningTable diningTable,
                            int rating, String comment, String customerName, String phone,
                            String language, Instant createdAt) {
        this.id = id;
        this.diningTable = diningTable;
        this.rating = rating;
        this.comment = comment;
        this.customerName = customerName;
        this.phone = phone;
        this.language = language;
        this.createdAt = createdAt;
    }

}

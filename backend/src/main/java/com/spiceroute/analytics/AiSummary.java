package com.spiceroute.analytics;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "ai_summaries")
public class AiSummary {
    @Id
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "input_hash", nullable = false)
    private String inputHash;

    @Column(nullable = false)
    private String result;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiSummary() {}

    public AiSummary(UUID id, UUID branchId, String inputHash, String result, Instant createdAt) {
        this.id = id;
        this.branchId = branchId;
        this.inputHash = inputHash;
        this.result = result;
        this.createdAt = createdAt;
    }

    public String getResult() { return result; }
}

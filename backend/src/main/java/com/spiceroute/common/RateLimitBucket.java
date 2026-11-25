package com.spiceroute.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "rate_limit_buckets")
public class RateLimitBucket {
    @Id
    @Column(name = "bucket_key", length = 64)
    private String bucketKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int hits;

    protected RateLimitBucket() {}

    public RateLimitBucket(String key, Instant expiresAt) {
        this.bucketKey = key;
        this.expiresAt = expiresAt;
        this.hits = 1;
    }

    public boolean consume(Instant now, Instant nextExpiry, int maximum) {
        if (!expiresAt.isAfter(now)) {
            expiresAt = nextExpiry;
            hits = 1;
            return true;
        }
        if (hits >= maximum) return false;
        hits++;
        return true;
    }
}

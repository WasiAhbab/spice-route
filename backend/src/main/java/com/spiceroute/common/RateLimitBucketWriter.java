package com.spiceroute.common;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class RateLimitBucketWriter {
    private final EntityManager entityManager;

    public RateLimitBucketWriter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void insert(RateLimitBucket bucket) {
        entityManager.persist(bucket);
        entityManager.flush();
    }
}

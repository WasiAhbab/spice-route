package com.spiceroute.common;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RateLimitRepository extends JpaRepository<RateLimitBucket, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bucket FROM RateLimitBucket bucket WHERE bucket.bucketKey = :key")
    Optional<RateLimitBucket> findLocked(@Param("key") String key);

    @Modifying
    @Query("DELETE FROM RateLimitBucket bucket WHERE bucket.expiresAt < :before")
    int deleteExpired(@Param("before") Instant before);
}

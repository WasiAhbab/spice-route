package com.spiceroute.common;

import com.spiceroute.security.SessionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseMaintenance {
    private final SessionRepository sessions;
    private final RateLimitRepository buckets;

    public DatabaseMaintenance(SessionRepository sessions, RateLimitRepository buckets) {
        this.sessions = sessions;
        this.buckets = buckets;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    @Transactional
    public void removeExpiredRecords() {
        Instant now = Instant.now();
        sessions.deleteExpired(now);
        buckets.deleteExpired(now.minus(1, ChronoUnit.HOURS));
    }
}

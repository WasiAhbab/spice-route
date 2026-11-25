package com.spiceroute.common;

import com.spiceroute.security.AuthService;

import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RateLimiter {
    private final RateLimitRepository buckets;
    private final RateLimitBucketWriter writer;
    private final TransactionTemplate transaction;

    public RateLimiter(RateLimitRepository buckets, RateLimitBucketWriter writer,
                       PlatformTransactionManager transactionManager) {
        this.buckets = buckets;
        this.writer = writer;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void enforce(String category, String subject, int maximum, Duration window) {
        String key = AuthService.hash(category + ":" + subject);
        boolean allowed;
        try {
            allowed = consume(key, maximum, window);
        } catch (DataIntegrityViolationException concurrentInsert) {
            // A concurrent request may have created the first bucket. Acquire its row lock and retry.
            allowed = consume(key, maximum, window);
        }
        if (!allowed) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Please try again later.");
    }

    private boolean consume(String key, int maximum, Duration window) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            Instant now = Instant.now();
            var existing = buckets.findLocked(key);
            if (existing.isPresent()) return existing.get().consume(now, now.plus(window), maximum);
            // Persist, not merge: concurrent inserts must conflict instead of overwriting counters.
            writer.insert(new RateLimitBucket(key, now.plus(window)));
            return true;
        }));
    }
}

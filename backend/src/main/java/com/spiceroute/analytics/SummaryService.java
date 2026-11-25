package com.spiceroute.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spiceroute.security.AuthService;
import com.spiceroute.common.RateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SummaryService {

    private final AnalyticsService analytics;
    private final AiSummaryRepository summaries;
    private final ObjectMapper json;
    private final CommentRedactor redactor;
    private final SummaryValidator validator;
    private final GeminiClient gemini;
    private final RateLimiter rateLimiter;
    private final Semaphore providerSlots = new Semaphore(4, true);
    private final java.util.Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public SummaryService(AnalyticsService analytics, AiSummaryRepository summaries, ObjectMapper json,
                          CommentRedactor redactor, SummaryValidator validator, GeminiClient gemini, RateLimiter rateLimiter) {
        this.analytics = analytics;
        this.summaries = summaries;
        this.json = json;
        this.redactor = redactor;
        this.validator = validator;
        this.gemini = gemini;
        this.rateLimiter = rateLimiter;
    }

    public SummaryReport generate(AuthService.Actor actor, UUID branch, LocalDate from, LocalDate to)
            throws JsonProcessingException {
        List<String> comments = analytics.feedback(actor, branch, from, to, null, null).stream()
            .map(redactor::redact).filter(redactor::meaningful).toList();
        if (comments.size() < 5) throw new IllegalArgumentException("At least five meaningful comments are required");
        if (comments.size() > 500) throw new IllegalArgumentException("Choose a shorter date range (maximum 500 comments)");
        String hash = AuthService.hash(json.writeValueAsString(List.of("report-v2", from, to, comments)));
        var cached = summaries.findByBranchIdAndInputHash(branch, hash);
        if (cached.isPresent()) return json.readValue(cached.get().getResult(), SummaryReport.class);

        String requestKey = branch + ":" + hash;
        if (!inFlight.add(requestKey)) throw new ResponseStatusException(HttpStatus.CONFLICT, "This report is already being generated");
        try {
            if (!providerSlots.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI is busy. Try again shortly.");
            try {
                rateLimiter.enforce("ai-user", actor.id().toString(), 5, Duration.ofMinutes(1));
                SummaryReport report = gemini.analyze(comments, validator);
                try {
                    summaries.saveAndFlush(new AiSummary(UUID.randomUUID(), branch, hash,
                        json.writeValueAsString(report), Instant.now()));
                } catch (DataIntegrityViolationException exception) {
                    var winner = summaries.findByBranchIdAndInputHash(branch, hash);
                    if (winner.isPresent()) return json.readValue(winner.get().getResult(), SummaryReport.class);
                    throw exception;
                }
                return report;
            } finally {
                providerSlots.release();
            }
        } finally {
            inFlight.remove(requestKey);
        }
    }
}

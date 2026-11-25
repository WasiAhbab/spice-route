package com.spiceroute.analytics;

import com.spiceroute.feedback.FeedbackRepository;
import com.spiceroute.feedback.FeedbackView;
import com.spiceroute.security.AccessService;
import com.spiceroute.security.AuthService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Dhaka");
    private final FeedbackRepository feedbackRepository;
    private final AccessService access;

    public AnalyticsService(FeedbackRepository feedbackRepository, AccessService access) {
        this.feedbackRepository = feedbackRepository;
        this.access = access;
    }

    public List<FeedbackView> feedback(AuthService.Actor actor, UUID branch, LocalDate from,
                                      LocalDate to, Integer rating, UUID table) {
        access.branch(actor, branch);
        if (from == null || to == null || from.isAfter(to) || from.plusYears(2).isBefore(to)) {
            throw new IllegalArgumentException("Choose a valid date range within two years");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        return feedbackRepository.findForBranch(branch, from.atStartOfDay(REPORT_ZONE).toInstant(),
            to.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant(), rating, table);
    }

    public record Review(String status, String note, Long version) {}

    @Transactional
    public void review(AuthService.Actor actor, UUID branchId, UUID feedbackId, Review entry) {
        access.branch(actor, branchId);
        var feedback = feedbackRepository.findById(feedbackId).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Feedback not found"));
        if (!feedback.getBranchId().equals(branchId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Feedback not found");
        }
        if (entry.version() == null || entry.version() != feedback.getVersion()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "This review changed. Refresh before editing again.");
        }
        if (entry.status() == null || !java.util.Set.of("NEW", "IN_REVIEW", "IN_PROGRESS", "RESOLVED").contains(entry.status())) {
            throw new IllegalArgumentException("Choose a valid review status");
        }
        String note = entry.note() == null ? "" : entry.note().strip();
        if (note.length() > 2000) throw new IllegalArgumentException("Internal note must be at most 2000 characters");
        feedback.review(entry.status(), note);
    }

    public List<BranchPerformance> comparison(AuthService.Actor actor) {
        return feedbackRepository.compareBranches(actor.id());
    }
}

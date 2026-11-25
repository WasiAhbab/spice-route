package com.spiceroute.feedback;

import com.spiceroute.management.DiningTable;
import com.spiceroute.management.DiningTableRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackService {

    private final DiningTableRepository tables;
    private final FeedbackRepository feedback;

    public FeedbackService(DiningTableRepository tables, FeedbackRepository feedback) {
        this.tables = tables;
        this.feedback = feedback;
    }

    public record Submission(int rating, String comment, String name, String phone, String language) {}

    @Transactional(readOnly = true)
    public Map<String, Object> lookup(UUID token) {
        DiningTable table = availableTable(token);
        var restaurant = table.getBranch().getRestaurant();
        return Map.of("id", table.getId(), "label", table.getLabel(),
            "branch", table.getBranch().getName(), "restaurant", restaurant.getName(),
            "welcome_message", restaurant.getWelcomeMessage(), "thank_you_message", restaurant.getThankYouMessage(),
            "collect_contact", restaurant.isCollectContact());
    }

    @Transactional
    public Object submit(UUID token, Submission submission) {
        DiningTable table = availableTable(token);
        if (submission.rating() < 1 || submission.rating() > 5 || submission.language() == null
                || !Set.of("en", "bn", "mixed").contains(submission.language())) {
            throw new IllegalArgumentException("Invalid rating or language");
        }
        boolean contact = table.getBranch().getRestaurant().isCollectContact();
        feedback.save(new CustomerFeedback(UUID.randomUUID(), table, submission.rating(),
            clean(submission.comment(), 2000), contact ? clean(submission.name(), 120) : null,
            contact ? clean(submission.phone(), 40) : null,
            submission.language(), Instant.now()));
        return Map.of("ok", true);
    }

    private DiningTable availableTable(UUID token) {
        return tables.findAvailableByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback link unavailable"));
    }

    private String clean(String value, int maximumLength) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > maximumLength) throw new IllegalArgumentException("Input too long");
        return value.trim();
    }
}

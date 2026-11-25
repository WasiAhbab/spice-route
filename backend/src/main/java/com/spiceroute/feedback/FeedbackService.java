package com.spiceroute.feedback;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.spiceroute.management.DiningTable;
import com.spiceroute.management.DiningTableRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackService {

    private static final PhoneNumberUtil PHONE_UTIL =
        PhoneNumberUtil.getInstance();

    private static final Set<String> SUPPORTED_LANGUAGES =
        Set.of("en", "bn", "mixed");

    private final DiningTableRepository tables;
    private final FeedbackRepository feedback;

    public FeedbackService(
        DiningTableRepository tables,
        FeedbackRepository feedback
    ) {
        this.tables = tables;
        this.feedback = feedback;
    }

    public record Submission(
        int rating,
        String comment,
        String name,
        String phone,
        String language
    ) {}

    @Transactional(readOnly = true)
    public Map<String, Object> lookup(UUID token) {
        DiningTable table = availableTable(token);

        var branch = table.getBranch();
        var restaurant = branch.getRestaurant();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", table.getId());
        response.put("label", table.getLabel());
        response.put("branch", branch.getName());
        response.put("restaurant", restaurant.getName());
        response.put("welcome_message", restaurant.getWelcomeMessage());
        response.put("thank_you_message", restaurant.getThankYouMessage());
        response.put("collect_contact", restaurant.isCollectContact());

        return response;
    }

    @Transactional
    public Object submit(UUID token, Submission submission) {
        if (submission == null) {
            throw new IllegalArgumentException(
                "Feedback submission is required."
            );
        }

        DiningTable table = availableTable(token);

        validateSubmission(submission);

        boolean collectContact = table
            .getBranch()
            .getRestaurant()
            .isCollectContact();

        String comment = clean(submission.comment(), 2000);

        String customerName = collectContact
            ? validateName(submission.name())
            : null;

        String customerPhone = collectContact
            ? validatePhone(submission.phone())
            : null;

        CustomerFeedback customerFeedback = new CustomerFeedback(
            UUID.randomUUID(),
            table,
            submission.rating(),
            comment,
            customerName,
            customerPhone,
            submission.language(),
            Instant.now()
        );

        feedback.save(customerFeedback);

        return Map.of("ok", true);
    }

    private void validateSubmission(Submission submission) {
        if (submission.rating() < 1 || submission.rating() > 5) {
            throw new IllegalArgumentException(
                "Rating must be between 1 and 5."
            );
        }

        if (submission.language() == null
                || !SUPPORTED_LANGUAGES.contains(submission.language())) {
            throw new IllegalArgumentException(
                "Invalid feedback language."
            );
        }
    }

    private String validateName(String value) {
        String name = clean(value, 120);

        if (name == null) {
            return null;
        }

        name = name.replaceAll(" +", " ");

        if (!name.matches(
                "\\p{L}[\\p{L}\\p{M}]*(?: +\\p{L}[\\p{L}\\p{M}]*)*")) {
            throw new IllegalArgumentException(
                "Name must contain only letters and spaces."
            );
        }

        return name;
    }

    private String validatePhone(String value) {
        String phone = clean(value, 16);

        if (phone == null) {
            return null;
        }

        if (!phone.matches("\\+[1-9][0-9]{1,14}")) {
            throw new IllegalArgumentException(
                "Enter a valid international phone number."
            );
        }

        try {
            var parsedPhone = PHONE_UTIL.parse(phone, "ZZ");

            if (!PHONE_UTIL.isValidNumber(parsedPhone)) {
                throw new IllegalArgumentException(
                    "Invalid phone number for the selected country."
                );
            }

            return PHONE_UTIL.format(
                parsedPhone,
                PhoneNumberUtil.PhoneNumberFormat.E164
            );

        } catch (NumberParseException exception) {
            throw new IllegalArgumentException(
                "Invalid phone number."
            );
        }
    }

    private DiningTable availableTable(UUID token) {
        return tables.findAvailableByToken(token)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Feedback link unavailable"
            ));
    }

    private String clean(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.strip();

        if (cleaned.length() > maximumLength) {
            throw new IllegalArgumentException(
                "Input exceeds the maximum allowed length of "
                    + maximumLength + " characters."
            );
        }

        return cleaned;
    }
}
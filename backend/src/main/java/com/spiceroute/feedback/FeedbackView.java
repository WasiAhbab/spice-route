package com.spiceroute.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record FeedbackView(
    UUID id,
    int rating,
    String comment,
    @JsonProperty("customer_name") String customerName,
    String phone,
    String language,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("table_label") String tableLabel,
    @JsonProperty("review_status") String reviewStatus,
    @JsonProperty("internal_note") String internalNote,
    long version
) {}

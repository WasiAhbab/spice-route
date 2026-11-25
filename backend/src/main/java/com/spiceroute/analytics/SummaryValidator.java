package com.spiceroute.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SummaryValidator {

    public SummaryReport validate(JsonNode result, List<String> comments) {
        if (result == null || !result.isObject() || !result.path("topics").isArray()) {
            throw new IllegalArgumentException("The AI response did not match the report structure");
        }
        String sentiment = result.path("sentiment").asText().toLowerCase(Locale.ROOT).trim();
        if (!Set.of("positive", "negative", "neutral", "mixed").contains(sentiment)) {
            throw new IllegalArgumentException("The AI response contained invalid sentiment");
        }
        List<SummaryReport.Topic> topics = new ArrayList<>();
        for (JsonNode topic : result.path("topics")) {
            String title = topic.path("title").asText().strip();
            String kind = topic.path("kind").asText().toLowerCase(Locale.ROOT).strip();
            String suggestion = topic.path("suggestion").asText().strip();
            if (title.isBlank() || title.length() > 180 || suggestion.isBlank() || suggestion.length() > 2000
                    || !Set.of("complaint", "praise").contains(kind) || !topic.path("ids").isArray()) continue;
            Set<Integer> ids = new LinkedHashSet<>();
            for (JsonNode id : topic.path("ids")) {
                if (id.isIntegralNumber() && id.canConvertToInt() && id.asInt() >= 0 && id.asInt() < comments.size()) {
                    ids.add(id.asInt());
                }
            }
            if (!ids.isEmpty()) {
                topics.add(new SummaryReport.Topic(title, kind, suggestion, ids.size(),
                    ids.stream().limit(3).map(comments::get).toList()));
            }
            if (topics.size() == 20) break;
        }
        if (topics.isEmpty()) throw new IllegalArgumentException("AI returned no supported findings");
        return new SummaryReport(sentiment, comments.size(), List.copyOf(topics));
    }
}

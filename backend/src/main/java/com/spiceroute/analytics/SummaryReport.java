package com.spiceroute.analytics;

import java.util.List;

public record SummaryReport(String sentiment, int commentCount, List<Topic> topics) {
    public record Topic(String title, String kind, String suggestion, int count, List<String> examples) {}
}

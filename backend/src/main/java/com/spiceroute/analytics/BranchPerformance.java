package com.spiceroute.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record BranchPerformance(UUID id, String name, long volume, Double average) {
    public BranchPerformance {
        if (average != null) {
            average = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
    }
}

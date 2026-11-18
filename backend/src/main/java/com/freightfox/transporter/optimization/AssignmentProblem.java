package com.freightfox.transporter.optimization;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable optimization input: every lane must be assigned to exactly one transporter that quoted for it,
 * using at most {@code maxTransporters} distinct transporters.
 */
public record AssignmentProblem(List<Long> laneIds, List<Quote> quotes, int maxTransporters) {

    public AssignmentProblem {
        if (laneIds == null || laneIds.isEmpty()) {
            throw new IllegalArgumentException("At least one lane is required");
        }
        if (maxTransporters < 1) {
            throw new IllegalArgumentException("maxTransporters must be at least 1");
        }
        laneIds = laneIds.stream().distinct().sorted().toList();
        // Deterministic order regardless of how the caller (or the database) ordered the quotes.
        quotes = quotes.stream()
                .sorted(Comparator.comparingLong(Quote::laneId).thenComparingLong(Quote::transporterId))
                .toList();
    }

    /** Distinct transporters that quoted at least once, ascending. Transporters with no quotes can never be used. */
    public List<Long> transporterIds() {
        return quotes.stream().map(Quote::transporterId).distinct().sorted().toList();
    }

    /** Lanes nobody quoted for; any such lane makes the problem infeasible. */
    public List<Long> uncoveredLaneIds() {
        Set<Long> quoted = quotes.stream().map(Quote::laneId).collect(Collectors.toSet());
        return laneIds.stream().filter(id -> !quoted.contains(id)).toList();
    }
}

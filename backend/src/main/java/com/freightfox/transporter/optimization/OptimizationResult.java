package com.freightfox.transporter.optimization;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Immutable optimizer output; assignments sorted by lane id, selected transporters ascending. */
public record OptimizationResult(List<Assignment> assignments, List<Long> selectedTransporters, BigDecimal totalCost) {

    public OptimizationResult {
        assignments = assignments.stream().sorted(Comparator.comparingLong(Assignment::laneId)).toList();
        selectedTransporters = selectedTransporters.stream().sorted().toList();
    }

    /** Builds a result from chosen quotes, deriving the selected transporters and the total cost. */
    public static OptimizationResult of(List<Quote> chosen) {
        return new OptimizationResult(
                chosen.stream().map(q -> new Assignment(q.laneId(), q.transporterId())).toList(),
                chosen.stream().map(Quote::transporterId).distinct().toList(),
                Money.normalize(chosen.stream().map(Quote::cost).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    public record Assignment(long laneId, long transporterId) {
    }
}

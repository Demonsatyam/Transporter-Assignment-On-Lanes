package com.freightfox.transporter.optimization;

import com.freightfox.transporter.exception.InvalidAssignmentException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Independent check of an optimizer result against the raw problem. Shares no code with the optimizer,
 * so a modelling bug cannot pass silently.
 */
public final class SolutionVerifier {

    private SolutionVerifier() {
    }

    /** @throws InvalidAssignmentException listing every violated rule */
    public static void verify(AssignmentProblem problem, OptimizationResult result) {
        Map<String, BigDecimal> priceOf = new HashMap<>();
        problem.quotes().forEach(q -> priceOf.put(q.laneId() + ":" + q.transporterId(), q.cost()));

        List<String> violations = new ArrayList<>();
        Set<Long> assignedLanes = new HashSet<>();
        Set<Long> usedTransporters = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OptimizationResult.Assignment a : result.assignments()) {
            if (!assignedLanes.add(a.laneId())) {
                violations.add("lane " + a.laneId() + " assigned more than once");
            }
            BigDecimal price = priceOf.get(a.laneId() + ":" + a.transporterId());
            if (price == null) {
                violations.add("transporter " + a.transporterId() + " has no quote for lane " + a.laneId());
            } else {
                total = total.add(price);
            }
            usedTransporters.add(a.transporterId());
        }

        Set<Long> missing = new HashSet<>(problem.laneIds());
        missing.removeAll(assignedLanes);
        if (!missing.isEmpty()) {
            violations.add("lanes not assigned: " + missing.stream().sorted().toList());
        }
        if (!problem.laneIds().containsAll(assignedLanes)) {
            violations.add("assignment contains unknown lanes");
        }
        if (!usedTransporters.equals(new HashSet<>(result.selectedTransporters()))) {
            violations.add("selectedTransporters " + result.selectedTransporters()
                    + " does not match transporters used in assignments " + usedTransporters);
        }
        if (usedTransporters.size() > problem.maxTransporters()) {
            violations.add(usedTransporters.size() + " transporters used, limit is " + problem.maxTransporters());
        }
        if (total.compareTo(result.totalCost()) != 0) {
            violations.add("totalCost " + result.totalCost() + " does not equal sum of assigned quotes " + total);
        }

        if (!violations.isEmpty()) {
            throw new InvalidAssignmentException(violations);
        }
    }
}

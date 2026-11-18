package com.freightfox.transporter.optimization;

import com.freightfox.transporter.exception.NoFeasibleAssignmentException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test oracle: enumerates every possible lane -> transporter assignment. Exponential, only for tiny inputs.
 * Shares no logic with the CP-SAT model, so agreement on the objective value is real evidence of optimality.
 */
final class BruteForceAssignmentOptimizer implements AssignmentOptimizer {

    @Override
    public OptimizationResult optimize(AssignmentProblem problem, ObjectivePolicy policy) {
        Map<Long, List<Quote>> byLane = problem.quotes().stream().collect(Collectors.groupingBy(Quote::laneId));
        List<List<Quote>> options = problem.laneIds().stream().map(l -> byLane.getOrDefault(l, List.of())).toList();
        List<Long> transporters = problem.transporterIds();

        Comparator<Candidate> order = policy == ObjectivePolicy.MAX_UTILIZATION_THEN_COST
                ? Comparator.comparingInt((Candidate c) -> -c.usage).thenComparing(c -> c.cost)
                : Comparator.comparing((Candidate c) -> c.cost).thenComparingInt(c -> -c.usage);
        order = order.thenComparingLong(c -> c.rankSum);

        Candidate best = null;
        int total = options.stream().mapToInt(List::size).reduce(1, (a, b) -> a * b);
        int[] index = new int[options.size()];
        for (int n = 0; n < total; n++) {
            List<Quote> chosen = new ArrayList<>();
            int rest = n;
            for (int i = 0; i < options.size(); i++) {
                index[i] = rest % options.get(i).size();
                rest /= options.get(i).size();
                chosen.add(options.get(i).get(index[i]));
            }
            Set<Long> used = chosen.stream().map(Quote::transporterId).collect(Collectors.toCollection(HashSet::new));
            if (used.size() > problem.maxTransporters()) {
                continue;
            }
            Candidate candidate = new Candidate(chosen, used.size(),
                    chosen.stream().map(Quote::cost).reduce(BigDecimal.ZERO, BigDecimal::add),
                    chosen.stream().mapToLong(q -> transporters.indexOf(q.transporterId())).sum());
            if (best == null || order.compare(candidate, best) < 0) {
                best = candidate;
            }
        }
        if (best == null) {
            throw new NoFeasibleAssignmentException("infeasible");
        }
        return OptimizationResult.of(best.chosen);
    }

    private record Candidate(List<Quote> chosen, int usage, BigDecimal cost, long rankSum) {
    }
}

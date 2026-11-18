package com.freightfox.transporter.optimization;

import com.freightfox.transporter.config.OptimizerProperties;
import com.freightfox.transporter.exception.NoFeasibleAssignmentException;
import com.freightfox.transporter.exception.OptimizationTimeoutException;
import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact optimizer backed by OR-Tools CP-SAT.
 *
 * <pre>
 * x(i,j) = 1 if lane i goes to transporter j (only for quoted pairs), y(j) = 1 if transporter j is used.
 *   Σ_j x(i,j) = 1          every lane assigned exactly once
 *   x(i,j) ≤ y(j)           only used transporters get lanes
 *   y(j) ≤ Σ_i x(i,j)       every used transporter gets at least one lane
 *   Σ_j y(j) ≤ K
 * </pre>
 *
 * Objectives are solved lexicographically per {@link ObjectivePolicy}; each optimum is fixed as a constraint
 * before the next stage. A final stage minimizes Σ x(i,j)·rank(j) (rank = position of j in ascending id order)
 * so ties prefer lower transporter ids. The solver runs single-threaded with a fixed seed, so results are
 * reproducible.
 */
@Component
public class CpSatAssignmentOptimizer implements AssignmentOptimizer {

    private static final Logger log = LoggerFactory.getLogger(CpSatAssignmentOptimizer.class);

    static {
        Loader.loadNativeLibraries();
    }

    private final double timeLimitSeconds;
    private final int workers;

    public CpSatAssignmentOptimizer(OptimizerProperties properties) {
        this.timeLimitSeconds = properties.timeLimitSeconds();
        this.workers = properties.solverWorkers();
    }

    @Override
    public OptimizationResult optimize(AssignmentProblem problem, ObjectivePolicy policy) {
        List<Long> uncovered = problem.uncoveredLaneIds();
        if (!uncovered.isEmpty()) {
            throw new NoFeasibleAssignmentException("No transporter has quoted for lane(s) " + uncovered + ".");
        }

        long deadline = System.nanoTime() + (long) (timeLimitSeconds * 1_000_000_000L);
        Model model = new Model(problem);
        model.cp.addLessOrEqual(model.usage(), problem.maxTransporters());

        boolean utilizationFirst = policy == ObjectivePolicy.MAX_UTILIZATION_THEN_COST;
        LinearExpr first = utilizationFirst ? model.usage() : model.cost();
        LinearExpr second = utilizationFirst ? model.cost() : model.usage();

        model.setObjective(first, utilizationFirst);
        CpSolver solver = solve(model, deadline, workers, false);
        if (solver == null) {
            throw infeasible(problem, deadline);
        }
        model.cp.addEquality(first, Math.round(solver.objectiveValue()));

        model.setObjective(second, !utilizationFirst);
        solver = solve(model, deadline, workers, false);
        model.cp.addEquality(second, Math.round(solver.objectiveValue()));

        // Tie-break: single worker so the choice among equally optimal solutions is reproducible. Every feasible
        // solution here already has the proven-optimal usage and cost, so running out of time only weakens
        // the tie-break, never the business objectives.
        model.setObjective(model.tieBreak(), false);
        CpSolver tieBroken = solve(model, deadline, 1, true);
        if (tieBroken != null) {
            solver = tieBroken;
        } else {
            log.warn("Tie-break stage did not finish in time; returning the cost-optimal solution as found");
        }

        List<Quote> chosen = new ArrayList<>();
        for (int q = 0; q < model.x.size(); q++) {
            if (solver.booleanValue(model.x.get(q))) {
                chosen.add(problem.quotes().get(q));
            }
        }
        OptimizationResult result = OptimizationResult.of(chosen);
        log.info("Optimized {} lanes / {} quotes, K={}, policy={}: {} transporters, totalCost={}",
                problem.laneIds().size(), problem.quotes().size(), problem.maxTransporters(), policy,
                result.selectedTransporters().size(), result.totalCost());
        return result;
    }

    /** Only the first stage can be infeasible, and only because K is below the minimum transporters needed. */
    private NoFeasibleAssignmentException infeasible(AssignmentProblem problem, long deadline) {
        Model unlimited = new Model(problem);
        unlimited.setObjective(unlimited.usage(), false);
        long required = Math.round(solve(unlimited, deadline, workers, false).objectiveValue());
        return new NoFeasibleAssignmentException("No feasible assignment exists: covering every lane needs at least "
                + required + " transporters, but maxTransporters is " + problem.maxTransporters() + ".");
    }

    /**
     * Returns the solver holding a proven optimum, or null if the model is infeasible. The optimum is fed back
     * as a hint, so the next stage starts from a solution that already satisfies its new equality constraint.
     * With {@code bestEffort}, a feasible but unproven solution is accepted and running out of time returns null.
     */
    private CpSolver solve(Model model, long deadline, int numWorkers, boolean bestEffort) {
        double remaining = (deadline - System.nanoTime()) / 1e9;
        if (remaining <= 0) {
            if (bestEffort) {
                return null;
            }
            throw new OptimizationTimeoutException(timeLimitSeconds);
        }
        CpSolver solver = new CpSolver();
        solver.getParameters()
                .setMaxTimeInSeconds(remaining)
                .setNumWorkers(numWorkers)
                // Interleaved search is deterministic regardless of thread count.
                .setInterleaveSearch(true)
                .setRandomSeed(42);
        long started = System.nanoTime();
        CpSolverStatus status = solver.solve(model.cp);
        log.debug("CP-SAT stage finished: {} in {} ms", status, (System.nanoTime() - started) / 1_000_000);
        return switch (status) {
            case OPTIMAL -> {
                model.hint(solver);
                yield solver;
            }
            case FEASIBLE -> {
                if (!bestEffort) {
                    // Optimality unproven: never return an unproven answer for a business objective.
                    throw new OptimizationTimeoutException(timeLimitSeconds);
                }
                yield solver;
            }
            case INFEASIBLE -> null;
            case UNKNOWN -> {
                if (!bestEffort) {
                    throw new OptimizationTimeoutException(timeLimitSeconds);
                }
                yield null;
            }
            default -> throw new IllegalStateException("CP-SAT returned " + status);
        };
    }

    /** The CP-SAT model and its variables; x is index-aligned with problem.quotes(). */
    private static final class Model {
        final CpModel cp = new CpModel();
        final List<BoolVar> x = new ArrayList<>();
        final List<BoolVar> y = new ArrayList<>();
        private final List<Quote> quotes;
        private final Map<Long, Integer> rank = new HashMap<>();

        Model(AssignmentProblem problem) {
            this.quotes = problem.quotes();
            List<Long> transporterIds = problem.transporterIds();
            for (int j = 0; j < transporterIds.size(); j++) {
                rank.put(transporterIds.get(j), j);
                y.add(cp.newBoolVar("y_" + transporterIds.get(j)));
            }

            Map<Long, List<Literal>> byLane = new HashMap<>();
            Map<Integer, LinearExprBuilder> usedImpliesAssigned = new HashMap<>();
            for (Quote quote : quotes) {
                BoolVar xq = cp.newBoolVar("x_" + quote.laneId() + "_" + quote.transporterId());
                x.add(xq);
                int j = rank.get(quote.transporterId());
                cp.addImplication(xq, y.get(j));
                byLane.computeIfAbsent(quote.laneId(), k -> new ArrayList<>()).add(xq);
                usedImpliesAssigned.computeIfAbsent(j, k -> LinearExpr.newBuilder().add(y.get(k))).addTerm(xq, -1);
            }
            byLane.values().forEach(cp::addExactlyOne);
            usedImpliesAssigned.values().forEach(expr -> cp.addLessOrEqual(expr, 0));
        }

        void setObjective(LinearExpr objective, boolean maximize) {
            cp.clearObjective();
            if (maximize) {
                cp.maximize(objective);
            } else {
                cp.minimize(objective);
            }
        }

        void hint(CpSolver solver) {
            cp.clearHints();
            x.forEach(v -> cp.addHint(v, solver.booleanValue(v) ? 1 : 0));
            y.forEach(v -> cp.addHint(v, solver.booleanValue(v) ? 1 : 0));
        }

        LinearExpr usage() {
            return LinearExpr.sum(y.toArray(BoolVar[]::new));
        }

        LinearExpr cost() {
            long[] paise = quotes.stream().mapToLong(q -> Money.toPaise(q.cost())).toArray();
            return LinearExpr.weightedSum(x.toArray(BoolVar[]::new), paise);
        }

        LinearExpr tieBreak() {
            long[] ranks = quotes.stream().mapToLong(q -> rank.get(q.transporterId())).toArray();
            return LinearExpr.weightedSum(x.toArray(BoolVar[]::new), ranks);
        }
    }
}

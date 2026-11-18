package com.freightfox.transporter.optimization;

import com.freightfox.transporter.config.OptimizerProperties;
import com.freightfox.transporter.exception.NoFeasibleAssignmentException;
import com.freightfox.transporter.exception.OptimizationTimeoutException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.freightfox.transporter.optimization.ObjectivePolicy.COST_THEN_UTILIZATION;
import static com.freightfox.transporter.optimization.ObjectivePolicy.MAX_UTILIZATION_THEN_COST;
import static com.freightfox.transporter.optimization.TestProblems.lanes;
import static com.freightfox.transporter.optimization.TestProblems.pdfExampleQuotes;
import static com.freightfox.transporter.optimization.TestProblems.q;
import static com.freightfox.transporter.optimization.TestProblems.testCase1Quotes;
import static com.freightfox.transporter.optimization.TestProblems.testCase2Quotes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class CpSatAssignmentOptimizerTest {

    private final CpSatAssignmentOptimizer optimizer =
            new CpSatAssignmentOptimizer(new OptimizerProperties(MAX_UTILIZATION_THEN_COST, 30, 8));

    private OptimizationResult solve(List<Long> lanes, List<Quote> quotes, int k, ObjectivePolicy policy) {
        AssignmentProblem problem = new AssignmentProblem(lanes, quotes, k);
        OptimizationResult result = optimizer.optimize(problem, policy);
        SolutionVerifier.verify(problem, result);
        return result;
    }

    private OptimizationResult solve(List<Long> lanes, List<Quote> quotes, int k) {
        return solve(lanes, quotes, k, MAX_UTILIZATION_THEN_COST);
    }

    private static List<OptimizationResult.Assignment> assignments(long... laneTransporterPairs) {
        List<OptimizationResult.Assignment> list = new ArrayList<>();
        for (int i = 0; i < laneTransporterPairs.length; i += 2) {
            list.add(new OptimizationResult.Assignment(laneTransporterPairs[i], laneTransporterPairs[i + 1]));
        }
        return list;
    }

    @Nested
    class PdfDatasets {

        @Test
        void apiExampleIsInfeasibleWithThreeTransporters() {
            // Lanes 5-6 only T3, 7-8 only T4, 9-10 only T5, plus one of {T1,T6} and one of {T2,T7}: at least 5.
            assertThatThrownBy(() -> solve(lanes(10), pdfExampleQuotes(), 3))
                    .isInstanceOf(NoFeasibleAssignmentException.class)
                    .hasMessageContaining("at least 5 transporters")
                    .hasMessageContaining("maxTransporters is 3");
        }

        @Test
        void apiExampleWithFiveTransporters() {
            OptimizationResult result = solve(lanes(10), pdfExampleQuotes(), 5);
            assertThat(result.totalCost()).isEqualByComparingTo("48000");
            assertThat(result.selectedTransporters()).containsExactly(1L, 2L, 3L, 4L, 5L);
        }

        @Test
        void apiExampleWithAllSevenUsesAllSeven() {
            OptimizationResult result = solve(lanes(10), pdfExampleQuotes(), 7);
            assertThat(result.selectedTransporters()).hasSize(7);
            assertThat(result.totalCost()).isEqualByComparingTo("48700");
        }

        @Test
        void testCase1WithThreeTransporters() {
            OptimizationResult result = solve(lanes(5), testCase1Quotes(), 3);
            assertThat(result.totalCost()).isEqualByComparingTo("60139");
            assertThat(result.assignments()).containsExactlyElementsOf(assignments(1, 5, 2, 1, 3, 4, 4, 4, 5, 4));
            assertThat(result.selectedTransporters()).containsExactly(1L, 4L, 5L);
        }

        @Test
        void testCase1PolicyChangesResultWhenObjectivesConflict() {
            OptimizationResult maxUsage = solve(lanes(5), testCase1Quotes(), 5, MAX_UTILIZATION_THEN_COST);
            assertThat(maxUsage.selectedTransporters()).hasSize(5);
            assertThat(maxUsage.totalCost()).isEqualByComparingTo("63388");

            OptimizationResult cheapest = solve(lanes(5), testCase1Quotes(), 5, COST_THEN_UTILIZATION);
            assertThat(cheapest.selectedTransporters()).hasSize(4);
            assertThat(cheapest.totalCost()).isEqualByComparingTo("59896");
        }

        @Test
        void testCase2() {
            assertThatThrownBy(() -> solve(lanes(5), testCase2Quotes(), 2))
                    .isInstanceOf(NoFeasibleAssignmentException.class)
                    .hasMessageContaining("at least 3 transporters");

            OptimizationResult three = solve(lanes(5), testCase2Quotes(), 3);
            assertThat(three.totalCost()).isEqualByComparingTo("7500");
            assertThat(three.selectedTransporters()).containsExactly(1L, 2L, 3L);

            OptimizationResult four = solve(lanes(5), testCase2Quotes(), 4);
            assertThat(four.totalCost()).isEqualByComparingTo("7450");
            assertThat(four.assignments()).containsExactlyElementsOf(assignments(1, 1, 2, 1, 3, 2, 4, 4, 5, 3));
        }
    }

    @Nested
    class Scenarios {

        @Test
        void oneLaneOneTransporter() {
            OptimizationResult result = solve(List.of(1L), List.of(q(1, 7, 900)), 1);
            assertThat(result.assignments()).containsExactlyElementsOf(assignments(1, 7));
            assertThat(result.totalCost()).isEqualByComparingTo("900");
        }

        @Test
        void limitOfOneForcesSingleTransporterCoveringAllLanes() {
            OptimizationResult result = solve(lanes(5), testCase1Quotes(), 1);
            assertThat(result.selectedTransporters()).containsExactly(4L);
            assertThat(result.totalCost()).isEqualByComparingTo("96940");
        }

        @Test
        void limitAboveAvailableTransportersUsesAllUsable() {
            OptimizationResult result = solve(lanes(5), testCase2Quotes(), 50);
            assertThat(result.selectedTransporters()).containsExactly(1L, 2L, 3L, 4L);
        }

        @Test
        void usageIsCappedByLaneCount() {
            List<Quote> quotes = List.of(q(1, 1, 10), q(1, 2, 20), q(1, 3, 30), q(2, 1, 10), q(2, 2, 20), q(2, 3, 30));
            OptimizationResult result = solve(lanes(2), quotes, 3);
            assertThat(result.selectedTransporters()).containsExactly(1L, 2L);
            assertThat(result.totalCost()).isEqualByComparingTo("30");
        }

        @Test
        void laneWithoutAnyQuoteIsInfeasible() {
            List<Quote> quotes = List.of(q(1, 1, 100), q(2, 1, 100));
            assertThatThrownBy(() -> solve(lanes(3), quotes, 5))
                    .isInstanceOf(NoFeasibleAssignmentException.class)
                    .hasMessageContaining("lane(s) [3]");
        }

        @Test
        void identicalQuotesBreakTiesTowardsLowerTransporterIds() {
            List<Quote> quotes = List.of(q(1, 9, 100), q(1, 3, 100), q(1, 5, 100));
            assertThat(solve(lanes(1), quotes, 1).assignments()).containsExactlyElementsOf(assignments(1, 3));
        }

        @Test
        void equallyOptimalSolutionsAreDeterministic() {
            List<Quote> quotes = new ArrayList<>();
            for (long lane = 1; lane <= 6; lane++) {
                for (long t = 1; t <= 6; t++) {
                    quotes.add(q(lane, t, 100));
                }
            }
            OptimizationResult first = solve(lanes(6), quotes, 3);
            for (int run = 0; run < 5; run++) {
                assertThat(solve(lanes(6), List.copyOf(quotes.reversed()), 3)).isEqualTo(first);
            }
            assertThat(first.selectedTransporters()).containsExactly(1L, 2L, 3L);
        }

        @Test
        void transportersWithUnusedQuotesAreNotSelected() {
            OptimizationResult result = solve(lanes(5), testCase1Quotes(), 3);
            assertThat(result.selectedTransporters()).doesNotContain(2L, 3L, 6L, 7L);
        }

        @Test
        void decimalQuotesAreExact() {
            List<Quote> quotes = List.of(
                    new Quote(1, 1, new BigDecimal("0.10")), new Quote(2, 1, new BigDecimal("0.20")),
                    new Quote(1, 2, new BigDecimal("0.15")));
            OptimizationResult result = solve(lanes(2), quotes, 2);
            assertThat(result.totalCost()).isEqualTo(new BigDecimal("0.35"));
        }

        @Test
        void largeSparseDatasetSolvesToProvenOptimum() {
            // 1000 lanes x 100 transporters, each transporter quoting ~10% of lanes (~10k quotes).
            List<Quote> quotes = randomQuotes(1000, 100, 0.1, new Random(7));
            OptimizationResult result = assertTimeoutPreemptively(Duration.ofSeconds(30),
                    () -> solve(lanes(1000), quotes, 100));
            assertThat(result.assignments()).hasSize(1000);
            assertThat(result.selectedTransporters()).hasSize(100);
        }

        @Test
        void hardInstanceBeyondTimeBudgetFailsInsteadOfReturningUnprovenAnswer() {
            CpSatAssignmentOptimizer impatient =
                    new CpSatAssignmentOptimizer(new OptimizerProperties(MAX_UTILIZATION_THEN_COST, 1, 8));
            // Dense quotes with a tight limit: a p-median-style instance that cannot be proven optimal in 1s.
            AssignmentProblem problem = new AssignmentProblem(lanes(300),
                    randomQuotes(300, 40, 0.4, new Random(7)), 10);
            assertThatThrownBy(() -> impatient.optimize(problem, MAX_UTILIZATION_THEN_COST))
                    .isInstanceOf(OptimizationTimeoutException.class);
        }

        private static List<Quote> randomQuotes(int laneCount, int transporterCount, double density, Random random) {
            List<Quote> quotes = new ArrayList<>();
            for (long lane = 1; lane <= laneCount; lane++) {
                for (long t = 1; t <= transporterCount; t++) {
                    // Every lane gets at least one quote so the instance is feasible for a large enough limit.
                    if (random.nextDouble() < density || t == lane % transporterCount + 1) {
                        quotes.add(q(lane, t, 1000 + random.nextInt(50_000)));
                    }
                }
            }
            return quotes;
        }
    }

    /** Compares against exhaustive enumeration on many small random instances, including heavy ties. */
    @ParameterizedTest
    @EnumSource(ObjectivePolicy.class)
    void matchesBruteForceOracleOnRandomInstances(ObjectivePolicy policy) {
        BruteForceAssignmentOptimizer oracle = new BruteForceAssignmentOptimizer();
        Random random = new Random(2024);
        int feasible = 0;
        for (int instance = 0; instance < 200; instance++) {
            int laneCount = 1 + random.nextInt(5);
            int transporterCount = 1 + random.nextInt(5);
            List<Quote> quotes = new ArrayList<>();
            for (long lane = 1; lane <= laneCount; lane++) {
                for (long t = 1; t <= transporterCount; t++) {
                    if (random.nextDouble() < 0.6) {
                        quotes.add(q(lane, t, 1 + random.nextInt(20)));
                    }
                }
            }
            AssignmentProblem problem = new AssignmentProblem(lanes(laneCount), quotes, 1 + random.nextInt(4));

            OptimizationResult expected;
            try {
                expected = oracle.optimize(problem, policy);
            } catch (NoFeasibleAssignmentException e) {
                assertThatThrownBy(() -> optimizer.optimize(problem, policy))
                        .as("instance %d should be infeasible", instance)
                        .isInstanceOf(NoFeasibleAssignmentException.class);
                continue;
            }
            OptimizationResult actual = optimizer.optimize(problem, policy);
            SolutionVerifier.verify(problem, actual);
            assertThat(actual.selectedTransporters()).as("instance %d usage", instance)
                    .hasSameSizeAs(expected.selectedTransporters());
            assertThat(actual.totalCost()).as("instance %d cost", instance).isEqualByComparingTo(expected.totalCost());
            assertThat(rankSum(problem, actual)).as("instance %d tie-break", instance)
                    .isEqualTo(rankSum(problem, expected));
            feasible++;
        }
        assertThat(feasible).as("enough feasible instances to be meaningful").isGreaterThan(50);
    }

    private static long rankSum(AssignmentProblem problem, OptimizationResult result) {
        List<Long> ids = problem.transporterIds();
        return result.assignments().stream().mapToLong(a -> ids.indexOf(a.transporterId())).sum();
    }
}

package com.freightfox.transporter.optimization;

import com.freightfox.transporter.exception.InvalidAssignmentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.freightfox.transporter.optimization.TestProblems.lanes;
import static com.freightfox.transporter.optimization.TestProblems.q;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolutionVerifierTest {

    private final AssignmentProblem problem = new AssignmentProblem(lanes(2),
            List.of(q(1, 1, 100), q(2, 1, 200), q(2, 2, 150)), 1);

    private static OptimizationResult result(List<OptimizationResult.Assignment> assignments, List<Long> selected,
                                             long total) {
        return new OptimizationResult(assignments, selected, BigDecimal.valueOf(total));
    }

    private static OptimizationResult.Assignment a(long lane, long transporter) {
        return new OptimizationResult.Assignment(lane, transporter);
    }

    @Test
    void acceptsValidSolution() {
        assertThatCode(() -> SolutionVerifier.verify(problem, result(List.of(a(1, 1), a(2, 1)), List.of(1L), 300)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAssignmentWithoutQuote() {
        assertThatThrownBy(() -> SolutionVerifier.verify(problem, result(List.of(a(1, 2), a(2, 2)), List.of(2L), 300)))
                .isInstanceOf(InvalidAssignmentException.class)
                .hasMessageContaining("transporter 2 has no quote for lane 1");
    }

    @Test
    void rejectsUncoveredLane() {
        assertThatThrownBy(() -> SolutionVerifier.verify(problem, result(List.of(a(1, 1)), List.of(1L), 100)))
                .hasMessageContaining("lanes not assigned: [2]");
    }

    @Test
    void rejectsTooManyTransporters() {
        assertThatThrownBy(() -> SolutionVerifier.verify(problem,
                result(List.of(a(1, 1), a(2, 2)), List.of(1L, 2L), 250)))
                .hasMessageContaining("2 transporters used, limit is 1");
    }

    @Test
    void rejectsSelectedTransporterThatIsNotUsed() {
        assertThatThrownBy(() -> SolutionVerifier.verify(problem,
                result(List.of(a(1, 1), a(2, 1)), List.of(1L, 2L), 300)))
                .hasMessageContaining("does not match transporters used");
    }

    @Test
    void rejectsWrongTotal() {
        assertThatThrownBy(() -> SolutionVerifier.verify(problem, result(List.of(a(1, 1), a(2, 1)), List.of(1L), 29000)))
                .hasMessageContaining("does not equal sum of assigned quotes 300");
    }
}

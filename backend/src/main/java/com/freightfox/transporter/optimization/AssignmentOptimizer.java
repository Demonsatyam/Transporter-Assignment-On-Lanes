package com.freightfox.transporter.optimization;

/**
 * Computes a globally optimal, full-coverage assignment.
 * Implementations throw {@link com.freightfox.transporter.exception.NoFeasibleAssignmentException}
 * when no assignment satisfies the constraints.
 */
public interface AssignmentOptimizer {

    OptimizationResult optimize(AssignmentProblem problem, ObjectivePolicy policy);
}

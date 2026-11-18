package com.freightfox.transporter.optimization;

/**
 * Which business objective wins when "use as many transporters as possible" and "lowest cost" disagree.
 * Objectives are applied lexicographically (solve, fix the optimum, solve the next), never blended with weights.
 */
public enum ObjectivePolicy {
    /** Maximize distinct transporters used (up to the limit), then minimize cost. */
    MAX_UTILIZATION_THEN_COST,
    /** Minimize cost, then maximize distinct transporters among the cheapest assignments. */
    COST_THEN_UTILIZATION
}

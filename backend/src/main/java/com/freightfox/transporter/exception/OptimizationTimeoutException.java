package com.freightfox.transporter.exception;

/** The solver could not prove an optimum within the configured time budget. */
public class OptimizationTimeoutException extends RuntimeException {

    public OptimizationTimeoutException(double timeLimitSeconds) {
        super("Optimization did not finish within " + timeLimitSeconds + " seconds.");
    }
}

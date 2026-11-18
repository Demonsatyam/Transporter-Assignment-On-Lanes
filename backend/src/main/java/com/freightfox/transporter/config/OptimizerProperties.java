package com.freightfox.transporter.config;

import com.freightfox.transporter.optimization.ObjectivePolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from {@code optimizer.*} in application.yml.
 *
 * @param timeLimitSeconds total solver budget per assignment request
 * @param solverWorkers    CP-SAT threads for the objective stages (the tie-break stage always uses one)
 */
@Validated
@ConfigurationProperties("optimizer")
public record OptimizerProperties(
        @NotNull ObjectivePolicy policy,
        @Positive double timeLimitSeconds,
        @Min(1) @Max(64) int solverWorkers) {
}

package com.freightfox.transporter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignmentRequest(
        @Schema(example = "3") @NotNull @Min(1) Integer maxTransporters,
        @Schema(description = "Dataset to optimize; defaults to the most recently submitted one", example = "1")
        @Positive Long datasetId) {
}

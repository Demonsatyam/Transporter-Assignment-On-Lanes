package com.freightfox.transporter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LaneRequest(
        @Schema(example = "1") @NotNull @Positive Long id,
        @Schema(example = "Mumbai") @NotBlank @Size(max = 100) String origin,
        @Schema(example = "Delhi") @NotBlank @Size(max = 100) String destination) {
}

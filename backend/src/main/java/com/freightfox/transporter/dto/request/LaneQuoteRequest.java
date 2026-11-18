package com.freightfox.transporter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LaneQuoteRequest(
        @Schema(example = "1") @NotNull @Positive Long laneId,
        @Schema(example = "5000", description = "Price in rupees; must be > 0 with at most 2 decimal places")
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal quote) {
}

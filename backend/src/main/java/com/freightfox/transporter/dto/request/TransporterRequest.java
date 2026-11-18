package com.freightfox.transporter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TransporterRequest(
        @Schema(example = "1") @NotNull @Positive Long id,
        @Schema(example = "Transporter T1") @NotBlank @Size(max = 150) String name,
        @Schema(description = "May be empty: such a transporter is simply never assigned")
        @NotNull List<@NotNull @Valid LaneQuoteRequest> laneQuotes) {
}

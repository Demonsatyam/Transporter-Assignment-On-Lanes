package com.freightfox.transporter.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TransporterInputRequest(
        @NotEmpty List<@NotNull @Valid LaneRequest> lanes,
        @NotEmpty List<@NotNull @Valid TransporterRequest> transporters) {
}

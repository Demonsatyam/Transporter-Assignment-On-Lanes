package com.freightfox.transporter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record AssignmentResponse(
        @Schema(example = "success") String status,
        @Schema(example = "60139") BigDecimal totalCost,
        List<AssignmentDetailResponse> assignments,
        @Schema(example = "[1, 4, 5]") List<Long> selectedTransporters) {
}

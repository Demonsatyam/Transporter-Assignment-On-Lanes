package com.freightfox.transporter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record InputResponse(
        @Schema(example = "success") String status,
        @Schema(example = "Input data saved successfully.") String message,
        @Schema(example = "1", description = "Pass to the assignment API to optimize this exact submission") Long datasetId) {

    public static InputResponse saved(Long datasetId) {
        return new InputResponse("success", "Input data saved successfully.", datasetId);
    }
}

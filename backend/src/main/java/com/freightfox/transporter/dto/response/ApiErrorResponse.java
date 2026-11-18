package com.freightfox.transporter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        @Schema(example = "error") String status,
        @Schema(example = "Validation failed") String message,
        List<String> errors) {

    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse("error", message, List.of());
    }

    public static ApiErrorResponse of(String message, List<String> errors) {
        return new ApiErrorResponse("error", message, errors);
    }
}

package com.freightfox.transporter.exception;

import com.freightfox.transporter.dto.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ApiErrorResponse.of("Validation failed", errors));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(InvalidInputException ex) {
        return respond(HttpStatus.BAD_REQUEST, ApiErrorResponse.of(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, ApiErrorResponse.of("Malformed JSON request body."));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ApiErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(NoFeasibleAssignmentException.class)
    public ResponseEntity<ApiErrorResponse> handleInfeasible(NoFeasibleAssignmentException ex) {
        return respond(HttpStatus.CONFLICT, ApiErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(OptimizationTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleTimeout(OptimizationTimeoutException ex) {
        log.warn(ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        // Spring MVC's own exceptions (404 route, 405 method, 415 media type, ...) carry their status.
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            String reason = springError.getBody().getTitle();
            return respond(status, ApiErrorResponse.of(reason != null ? reason : "Request failed."));
        }
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorResponse.of("An unexpected error occurred."));
    }

    private static ResponseEntity<ApiErrorResponse> respond(HttpStatusCode status, ApiErrorResponse body) {
        return ResponseEntity.status(status).body(body);
    }
}

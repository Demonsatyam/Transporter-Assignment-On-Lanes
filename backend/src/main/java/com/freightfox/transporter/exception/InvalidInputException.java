package com.freightfox.transporter.exception;

import java.util.List;

/** Cross-field input errors (duplicates, dangling references) that bean validation cannot express. */
public class InvalidInputException extends RuntimeException {

    private final List<String> errors;

    public InvalidInputException(List<String> errors) {
        super("Validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}

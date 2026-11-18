package com.freightfox.transporter.exception;

import java.util.List;

/** The optimizer produced a result that fails independent verification: an internal bug, never a client error. */
public class InvalidAssignmentException extends RuntimeException {

    public InvalidAssignmentException(List<String> violations) {
        super("Optimizer produced an invalid assignment: " + String.join("; ", violations));
    }
}

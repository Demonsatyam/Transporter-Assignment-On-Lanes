package com.freightfox.transporter.exception;

/** The request is valid but no assignment can cover every lane within the transporter limit. */
public class NoFeasibleAssignmentException extends RuntimeException {

    public NoFeasibleAssignmentException(String message) {
        super(message);
    }
}

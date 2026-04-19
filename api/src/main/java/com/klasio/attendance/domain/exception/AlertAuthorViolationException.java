package com.klasio.attendance.domain.exception;

public class AlertAuthorViolationException extends RuntimeException {

    public AlertAuthorViolationException() {
        super("Only the alert author can update the reason");
    }
}

package com.klasio.shared.infrastructure.exception;

public class SessionInPastException extends RuntimeException {

    public SessionInPastException(String message) {
        super(message);
    }
}

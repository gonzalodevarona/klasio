package com.klasio.shared.infrastructure.exception;

public class SessionAlreadyStartedException extends RuntimeException {
    public SessionAlreadyStartedException() {
        super("Session has already started");
    }
}

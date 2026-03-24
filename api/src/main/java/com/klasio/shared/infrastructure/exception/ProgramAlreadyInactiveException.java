package com.klasio.shared.infrastructure.exception;

public class ProgramAlreadyInactiveException extends RuntimeException {

    public ProgramAlreadyInactiveException(String message) {
        super(message);
    }
}

package com.klasio.shared.infrastructure.exception;

public class ProgramAlreadyActiveException extends RuntimeException {

    public ProgramAlreadyActiveException(String message) {
        super(message);
    }
}

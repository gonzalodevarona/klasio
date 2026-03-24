package com.klasio.shared.infrastructure.exception;

public class ProgramNameAlreadyExistsException extends RuntimeException {

    public ProgramNameAlreadyExistsException(String message) {
        super(message);
    }
}
